###########################################################################
# A module to create API dump from disassembled code
#
# Copyright (C) 2016-2017 Andrey Ponomarenko's ABI Laboratory
#
# Written by Andrey Ponomarenko
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License or the GNU Lesser
# General Public License as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# and the GNU Lesser General Public License along with this program.
# If not, see <http://www.gnu.org/licenses/>.
###########################################################################
use strict;
use IPC::Open3;

my $ExtractCounter = 0;

my %MName_Mid;
my %Mid_MName;

my $T_ID = 0;
my $M_ID = 0;
my $U_ID = 0;

# Aliases
my (%MethodInfo, %TypeInfo, %TName_Tid) = ();

foreach (1, 2)
{
    $MethodInfo{$_} = $In::API{$_}{"MethodInfo"};
    $TypeInfo{$_} = $In::API{$_}{"TypeInfo"};
    $TName_Tid{$_} = $In::API{$_}{"TName_Tid"};
}

sub createAPIDump($)
{
    my $LVer = $_[0];
    
    readArchives($LVer);
    
    if(not keys(%{$MethodInfo{$LVer}})) {
        printMsg("WARNING", "empty dump");
    }
    
    $In::API{$LVer}{"LibraryVersion"} = $In::Desc{$LVer}{"Version"};
    $In::API{$LVer}{"LibraryName"} = $In::Opt{"TargetLib"};
    $In::API{$LVer}{"Language"} = "Java";
}

sub readArchives($)
{
    my $LVer = $_[0];
    my @ArchivePaths = getArchives($LVer);
    if($#ArchivePaths==-1) {
        exitStatus("Error", "Java archives are not found in ".$In::Desc{$LVer}{"Version"});
    }
    printMsg("INFO", "Reading classes ".$In::Desc{$LVer}{"Version"}." ...");
    
    $T_ID = 0;
    $M_ID = 0;
    $U_ID = 0;
    
    %MName_Mid = ();
    %Mid_MName = ();
    
    foreach my $ArchivePath (sort {length($a)<=>length($b)} @ArchivePaths) {
        readArchive($LVer, $ArchivePath);
    }
    foreach my $TName (keys(%{$TName_Tid{$LVer}}))
    {
        my $Tid = $TName_Tid{$LVer}{$TName};
        if(not $TypeInfo{$LVer}{$Tid}{"Type"})
        {
            if($TName=~/\A(void|boolean|char|byte|short|int|float|long|double)\Z/) {
                $TypeInfo{$LVer}{$Tid}{"Type"} = "primitive";
            }
            else {
                $TypeInfo{$LVer}{$Tid}{"Type"} = "class";
            }
        }
    }
}

sub getArchives($)
{
    my $LVer = $_[0];
    my @Paths = ();
    foreach my $Path (keys(%{$In::Desc{$LVer}{"Archives"}}))
    {
        if(not -e $Path) {
            exitStatus("Access_Error", "can't access \'$Path\'");
        }
        
        foreach (getArchivePaths($Path, $LVer)) {
            push(@Paths, $_);
        }
    }
    return @Paths;
}

sub readArchive($$)
{ # 1, 2 - library, 0 - client
    my ($LVer, $Path) = @_;
    
    $Path = getAbsPath($Path);
    my $JarCmd = getCmdPath("jar");
    if(not $JarCmd) {
        exitStatus("Not_Found", "can't find \"jar\" command");
    }
    my $ExtractPath = join_P($In::Opt{"Tmp"}, $ExtractCounter);
    if(-d $ExtractPath) {
        rmtree($ExtractPath);
    }
    mkpath($ExtractPath);
    chdir($ExtractPath);
    system($JarCmd." -xf \"$Path\"");
    if($?) {
        exitStatus("Error", "can't extract \'$Path\'");
    }
    chdir($In::Opt{"OrigDir"});
    my @Classes = ();
    foreach my $ClassPath (cmdFind($ExtractPath, "", "*\\.class"))
    {
        if($In::Opt{"OS"} ne "windows") {
            $ClassPath=~s/\.class\Z//g;
        }
        
        my $ClassName = getFilename($ClassPath);
        if($ClassName=~/\$\d/ or $ClassName eq "module-info") {
            next;
        }
        
        $ClassPath = cutPrefix($ClassPath, $ExtractPath); # javap decompiler accepts relative paths only
        
        my $ClassDir = getDirname($ClassPath);
        if($ClassDir=~/\./)
        { # jaxb-osgi.jar/1.0/org/apache
            next;
        }
        
        my $Package = getPFormat($ClassDir);
        if($LVer)
        {
            if(skipPackage($Package, $LVer))
            { # internal packages
                next;
            }
        }
        
        push(@Classes, $ClassPath);
    }
    
    if($#Classes!=-1)
    {
        foreach my $PartRef (divideArray(\@Classes))
        {
            if($LVer) {
                readClasses($PartRef, $LVer, getFilename($Path));
            }
            else {
                readClasses_Usage($PartRef);
            }
        }
    }
    
    $ExtractCounter += 1;
    
    if($LVer)
    {
        foreach my $SubArchive (cmdFind($ExtractPath, "", "*\\.jar"))
        { # recursive step
            readArchive($LVer, $SubArchive);
        }
    }
    
    rmtree($ExtractPath);
}

sub sepParams($$$)
{
    my ($Params, $Comma, $Sp) = @_;
    my @Parts = ();
    my %B = ( "("=>0, "<"=>0, ")"=>0, ">"=>0 );
    my $Part = 0;
    foreach my $Pos (0 .. length($Params) - 1)
    {
        my $S = substr($Params, $Pos, 1);
        if(defined $B{$S}) {
            $B{$S} += 1;
        }
        if($S eq "," and
        $B{"("}==$B{")"} and $B{"<"}==$B{">"})
        {
            if($Comma)
            { # include comma
                $Parts[$Part] .= $S;
            }
            $Part += 1;
        }
        else {
            $Parts[$Part] .= $S;
        }
    }
    if(not $Sp)
    { # remove spaces
        foreach (@Parts)
        {
            s/\A //g;
            s/ \Z//g;
        }
    }
    return @Parts;
}

sub simpleDecl($$)
{
    my ($Line, $LVer) = @_;
    
    my %B = ( "<"=>0, ">"=>0 );
    my @Chars = split("", $Line);
    
    my $Extends = undef;
    my ($Pre, $Post) = ("", "");
    my @Replace = ();
    
    foreach my $Pos (0 .. $#Chars)
    {
        my $S = $Chars[$Pos];
        if(defined $B{$S}) {
            $B{$S} += 1;
        }
        
        if($B{"<"}!=0)
        {
            if(defined $Extends)
            {
                my $E = 0;
                
                if($S eq ",")
                {
                    if($B{"<"}-$B{">"}==$Extends) {
                        $E = 1;
                    }
                }
                elsif($S eq ">")
                {
                    if($B{"<"}==$B{">"} or $B{"<"}-$B{">"}+1==$Extends) {
                        $E = 1;
                    }
                }
                
                if($E)
                {
                    if($Post) {
                        push(@Replace, $Post);
                    }
                    
                    $Extends = undef;
                    ($Pre, $Post) = ("", "");
                }
            }
            elsif($B{"<"}!=$B{">"})
            {
                if(substr($Pre, -9) eq " extends ")
                {
                    $Extends = $B{"<"}-$B{">"};
                }
            }
        }
        
        $Pre .= $S;
        if(defined $Extends) {
            $Post .= $S;
        }
    }
    
    my %Tmpl = ();
    
    foreach my $R (@Replace)
    {
        if($Line=~s/([A-Za-z\d\?]+) extends \Q$R\E/$1/) {
            $Tmpl{$1} = registerType($R, $LVer);
        }
    }
    
    return ($Line, \%Tmpl);
}

sub readClasses($$$)
{
    my ($Paths, $LVer, $ArchiveName) = @_;
    
    my $JavapCmd = getCmdPath("javap");
    if(not $JavapCmd) {
        exitStatus("Not_Found", "can't find \"javap\" command");
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    # ! private info should be processed
    my @Cmd = ($JavapCmd, "-s", "-private");
    if(not $In::Opt{"Quick"}) {
        @Cmd = (@Cmd, "-c", "-verbose");
    }
    
    @Cmd = (@Cmd, @{$Paths});
    
    chdir($TmpDir."/".$ExtractCounter);
    my $Pid = open3(*IN, *OUT, *ERR, @Cmd);
    close(IN);
    
    my (%TypeAttr, $CurrentMethod, $CurrentPackage, $CurrentClass, $CurrentClass_Short) = ();
    my ($InParamTable, $InVarTypeTable, $InExceptionTable, $InCode) = (0, 0, 0, 0);
    
    my $InAnnotations = undef;
    my $InAnnotations_Class = undef;
    my $InAnnotations_Method = undef;
    my %AnnotationName = ();
    my %AnnotationNum = (); # support for Java 7
    
    my ($ParamPos, $FieldPos) = (0, 0);
    my ($LINE, $Stay, $Run, $NonEmpty) = (undef, 0, 1, undef);
    
    my $DContent = "";
    my $Debug = (defined $In::Opt{"Debug"});
    
    while($Run)
    {
        if(not $Stay)
        {
            $LINE = <OUT>;
            
            if(not defined $NonEmpty and $LINE) {
                $NonEmpty = 1;
            }
            
            if($Debug) {
                $DContent .= $LINE;
            }
        }
        
        if(not $LINE)
        {
            $Run = 0;
            last;
        }
        
        $Stay = 0;
        
        if($LINE=~/\A\s*const/) {
            next;
        }
        
        if(index($LINE, 'Start  Length')!=-1
        or index($LINE, 'Compiled from')!=-1
        or index($LINE, 'Last modified')!=-1
        or index($LINE, 'MD5 checksum')!=-1
        or index($LINE, 'Classfile /')==0
        or index($LINE, 'Classfile jar')==0)
        {
            next;
        }
        
        if(index($LINE, '=')!=-1)
        {
            if(index($LINE, ' stack=')!=-1
            or index($LINE, 'frame_type =')!=-1
            or index($LINE, 'offset_delta =')!=-1)
            {
                next;
            }
        }
        
        if(index($LINE, ':')!=-1)
        {
            if(index($LINE, ' LineNumberTable:')!=-1
            or index($LINE, 'SourceFile:')==0
            or index($LINE, ' StackMapTable:')!=-1
            or index($LINE, ' Exceptions:')!=-1
            or index($LINE, 'Constant pool:')!=-1
            or index($LINE, 'minor version:')!=-1
            or index($LINE, 'major version:')!=-1
            or index($LINE, ' AnnotationDefault:')!=-1)
            {
                next;
            }
        }
        
        if(index($LINE, " of ")!=-1
        or index($LINE, "= [")!=-1) {
            next;
        }
        
        if($LINE=~/ line \d+:|\[\s*class|\$\d|\._\d/)
        { # artificial methods and code
            next;
        }
        
        # $LINE=~s/ \$(\w)/ $1/g;
        
        if(index($LINE, '$')!=-1)
        {
            if(index($LINE, ' class$')!=-1
            or index($LINE, '$eq')!=-1
            or index($LINE, '.$')!=-1
            or index($LINE, '/$')!=-1
            or index($LINE, '$$')!=-1
            or index($LINE, '$(')!=-1
            or index($LINE, '$:')!=-1
            or index($LINE, '$.')!=-1
            or index($LINE, '$;')!=-1) {
                next;
            }
            
            if($LINE=~/ (\w+\$|)\w+\$\w+[\(:]/) {
                next;
            }
            
            if(not $InParamTable and not $InVarTypeTable)
            {
                if(index($LINE, ' $')!=-1) {
                    next;
                }
            }
            
            $LINE=~s/\$([\> ]|\s*\Z)/$1/g;
        }
        
        my $EndBr = ($LINE eq "}\n" or $LINE eq "}\r\n");
        
        if($EndBr) {
            $InAnnotations_Class = 1;
        }
        
        if($EndBr or $LINE eq "\n" or $LINE eq "\r\n")
        {
            $CurrentMethod = undef;
            $InCode = 0;
            $InAnnotations_Method = 0;
            $InParamTable = 0;
            $InVarTypeTable = 0;
            next;
        }
        
        if(index($LINE, '#')!=-1)
        {
            if($LINE=~/\A\s*#(\d+)/)
            { # Constant pool
                my $CNum = $1;
                if($LINE=~/\s+([^ ]+?);/)
                {
                    my $AName = $1;
                    $AName=~s/\AL//;
                    $AName=~s/\$/./g;
                    $AName=~s/\//./g;
                    
                    $AnnotationName{$CNum} = $AName;
                    
                    if(defined $AnnotationNum{$CNum})
                    { # support for Java 7
                        if($InAnnotations_Class) {
                            $TypeAttr{"Annotations"}{registerType($AName, $LVer)} = 1;
                        }
                        delete($AnnotationNum{$CNum});
                    }
                }
                
                next;
            }
            
            if(index($LINE, ": #")!=-1 and index($LINE, "//")!=-1) {
                next;
            }
        }
        
        my $TmplP = undef;
        
        # Java 7: templates
        if(index($LINE, "<")!=-1)
        { # <T extends java.lang.Object>
          # <KEYIN extends java.lang.Object ...
            if($LINE=~/<[A-Z\d\?]+ /i) {
                ($LINE, $TmplP) = simpleDecl($LINE, $LVer);
            }
        }
        
        if(index($LINE, ',')!=-1) {
            $LINE=~s/\s*,\s*/,/g;
        }
        
        if(index($LINE, '$')!=-1) {
            $LINE=~s/\$/#/g;
        }
        
        if(index($LINE, "LocalVariableTable")!=-1) {
            $InParamTable += 1;
        }
        elsif(index($LINE, "LocalVariableTypeTable")!=-1) {
            $InVarTypeTable += 1;
        }
        elsif(index($LINE, "Exception table")!=-1) {
            $InExceptionTable = 1;
        }
        elsif(index($LINE, " Code:")!=-1)
        {
            $InCode += 1;
            $InAnnotations = undef;
        }
        elsif(index($LINE, ':')!=-1
        and $LINE=~/\A\s*\d+:\s*/)
        { # read Code
            if($InCode==1)
            {
                if($CurrentMethod)
                {
                    if(index($LINE, "invoke")!=-1)
                    {
                        if($LINE=~/ invoke(\w+) .* \/\/\s*(Method|InterfaceMethod)\s+(.+?)\s*\Z/)
                        { # 3:   invokevirtual   #2; //Method "[Lcom/sleepycat/je/Database#DbState;".clone:()Ljava/lang/Object;
                            my ($InvokeType, $InvokedName) = ($1, $3);
                            
                            if($InvokedName!~/\A(\w+:|java\/(lang|util|io)\/)/
                            and index($InvokedName, '"<init>":')!=0)
                            {
                                $InvokedName=~s/#/\$/g;
                                
                                my $ID = undef;
                                if($In::Opt{"Reproducible"}) {
                                    $ID = getMd5($InvokedName);
                                }
                                else {
                                    $ID = ++$U_ID;
                                }
                                
                                $In::API{$LVer}{"MethodUsed"}{$ID}{"Name"} = $InvokedName;
                                $In::API{$LVer}{"MethodUsed"}{$ID}{"Used"}{$CurrentMethod} = $InvokeType;
                            }
                        }
                    }
                    # elsif($LINE=~/ (getstatic|putstatic) .* \/\/\s*Field\s+(.+?)\s*\Z/)
                    # {
                    #     my $UsedFieldName = $2;
                    #     $In::API{$LVer}{"FieldUsed"}{$UsedFieldName}{$CurrentMethod} = 1;
                    # }
                }
            }
            elsif(defined $InAnnotations)
            {
                if($LINE=~/\A\s*\d+\:\s*#(\d+)/)
                {
                    if(my $AName = $AnnotationName{$1})
                    {
                        if($InAnnotations_Class) {
                            $TypeAttr{"Annotations"}{registerType($AName, $LVer)} = 1;
                        }
                        elsif($InAnnotations_Method) {
                            $MethodInfo{$LVer}{$MName_Mid{$CurrentMethod}}{"Annotations"}{registerType($AName, $LVer)} = 1;
                        }
                    }
                    else
                    { # suport for Java 7
                        $AnnotationNum{$1} = 1;
                    }
                }
            }
        }
        elsif($InParamTable==1 and $LINE=~/\A\s+\d/)
        { # read parameter names from LocalVariableTable
            if($CurrentMethod and $LINE=~/\A\s+0\s+\d+\s+\d+\s+(\#?)(\w+)/)
            {
                my $Art = $1;
                my $PName = $2;
                
                if(($PName ne "this" or $Art) and $PName=~/[a-z]/i)
                {
                    if($CurrentMethod)
                    {
                        my $ID = $MName_Mid{$CurrentMethod};
                        
                        if(defined $MethodInfo{$LVer}{$ID}
                        and defined $MethodInfo{$LVer}{$ID}{"Param"}
                        and defined $MethodInfo{$LVer}{$ID}{"Param"}{$ParamPos}
                        and defined $MethodInfo{$LVer}{$ID}{"Param"}{$ParamPos}{"Type"})
                        {
                            $MethodInfo{$LVer}{$ID}{"Param"}{$ParamPos}{"Name"} = $PName;
                            $ParamPos++;
                        }
                    }
                }
            }
        }
        elsif($InVarTypeTable==1 and $LINE=~/\A\s+\d/)
        {
            # skip
        }
        elsif($CurrentClass and index($LINE, '(')!=-1
        and $LINE=~/(\A|\s+)([^\s]+)\s+([^\s]+)\s*\((.*)\)\s*(throws\s*([^\s]+)|)\s*;\s*\Z/)
        { # attributes of methods and constructors
            my (%MethodAttr, $ParamsLine, $Exceptions) = ();
            
            $InParamTable = 0; # read the first local variable table
            $InVarTypeTable = 0;
            $InCode = 0; # read the first code
            $InAnnotations_Method = 1;
            $InAnnotations_Class = 0;
            
            ($MethodAttr{"Return"}, $MethodAttr{"ShortName"}, $ParamsLine, $Exceptions) = ($2, $3, $4, $6);
            $MethodAttr{"ShortName"}=~s/#/./g;
            
            if($Exceptions)
            {
                foreach my $E (split(/,/, $Exceptions)) {
                    $MethodAttr{"Exceptions"}{registerType($E, $LVer)} = 1;
                }
            }
            if($LINE=~/(\A|\s+)(public|protected|private)\s+/) {
                $MethodAttr{"Access"} = $2;
            }
            else {
                $MethodAttr{"Access"} = "package-private";
            }
            $MethodAttr{"Class"} = registerType($TypeAttr{"Name"}, $LVer);
            if($MethodAttr{"ShortName"}=~/\A(|(.+)\.)(\Q$CurrentClass\E|\Q$CurrentClass_Short\E)\Z/)
            {
                if($2)
                {
                    $MethodAttr{"Package"} = $2;
                    $CurrentPackage = $MethodAttr{"Package"};
                    $MethodAttr{"ShortName"} = $CurrentClass;
                }
                $MethodAttr{"Constructor"} = 1;
                delete($MethodAttr{"Return"});
            }
            else
            {
                $MethodAttr{"Return"} = registerType($MethodAttr{"Return"}, $LVer);
            }
            
            my @Params = sepParams($ParamsLine, 0, 1);
            
            $ParamPos = 0;
            foreach my $ParamTName (@Params)
            {
                %{$MethodAttr{"Param"}{$ParamPos}} = ("Type"=>registerType($ParamTName, $LVer), "Name"=>"p".($ParamPos+1));
                $ParamPos++;
            }
            $ParamPos = 0;
            if(not $MethodAttr{"Constructor"})
            { # methods
                if($CurrentPackage) {
                    $MethodAttr{"Package"} = $CurrentPackage;
                }
                if($LINE=~/(\A|\s+)abstract\s+/) {
                    $MethodAttr{"Abstract"} = 1;
                }
                if($LINE=~/(\A|\s+)final\s+/) {
                    $MethodAttr{"Final"} = 1;
                }
                if($LINE=~/(\A|\s+)static\s+/) {
                    $MethodAttr{"Static"} = 1;
                }
                if($LINE=~/(\A|\s+)native\s+/) {
                    $MethodAttr{"Native"} = 1;
                }
                if($LINE=~/(\A|\s+)synchronized\s+/) {
                    $MethodAttr{"Synchronized"} = 1;
                }
            }
            
            my $LINE_N = <OUT>;
            
            if($Debug) {
                $DContent .= $LINE_N;
            }
            
            # $LINE_N=~s/ \$(\w)/ $1/g;
            $LINE_N=~s/\$([\> ]|\s*\Z)/$1/g;
            
            # read the Signature
            if(index($LINE_N, ": #")==-1
            and $LINE_N=~/(Signature|descriptor):\s*(.+?)\s*\Z/i)
            { # create run-time unique name ( java/io/PrintStream.println (Ljava/lang/String;)V )
                my $SignParams = $2;
                
                # Generic classes
                my $ShortClass = $CurrentClass;
                $ShortClass=~s/<.*>//g;
                
                if($MethodAttr{"Constructor"}) {
                    $CurrentMethod = $ShortClass.".\"<init>\":".$SignParams;
                }
                else {
                    $CurrentMethod = $ShortClass.".".$MethodAttr{"ShortName"}.":".$SignParams;
                }
                if(my $PackageName = getSFormat($CurrentPackage)) {
                    $CurrentMethod = $PackageName."/".$CurrentMethod;
                }
            }
            else {
                exitStatus("Error", "internal error - can't read method signature");
            }
            
            $MethodAttr{"Archive"} = $ArchiveName;
            if($CurrentMethod)
            {
                my $ID = undef;
                if($In::Opt{"Reproducible"}) {
                    $ID = getMd5($CurrentMethod);
                }
                else {
                    $ID = ++$M_ID;
                }
                
                $MName_Mid{$CurrentMethod} = $ID;
                
                if(defined $Mid_MName{$ID} and $Mid_MName{$ID} ne $CurrentMethod) {
                    printMsg("ERROR", "md5 collision on \'$ID\', please increase ID length (MD5_LEN in Basic.pm)");
                }
                
                $Mid_MName{$ID} = $CurrentMethod;
                
                $MethodAttr{"Name"} = $CurrentMethod;
                $MethodInfo{$LVer}{$ID} = \%MethodAttr;
            }
        }
        elsif($CurrentClass and $LINE=~/(\A|\s+)([^\s]+)\s+(\w+);\s*\Z/)
        { # fields
            my ($TName, $FName) = ($2, $3);
            $TypeAttr{"Fields"}{$FName}{"Type"} = registerType($TName, $LVer);
            if($LINE=~/(\A|\s+)final\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Final"} = 1;
            }
            if($LINE=~/(\A|\s+)static\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Static"} = 1;
            }
            if($LINE=~/(\A|\s+)transient\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Transient"} = 1;
            }
            if($LINE=~/(\A|\s+)volatile\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Volatile"} = 1;
            }
            if($LINE=~/(\A|\s+)(public|protected|private)\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Access"} = $2;
            }
            else {
                $TypeAttr{"Fields"}{$FName}{"Access"} = "package-private";
            }
            
            $TypeAttr{"Fields"}{$FName}{"Pos"} = $FieldPos++;
            
            my $LINE_NP = <OUT>;
            if($Debug) {
                $DContent .= $LINE_NP;
            }
            
            # read the Signature
            if(index($LINE_NP, ": #")==-1
            and $LINE_NP=~/(Signature|descriptor):\s*(.+?)\s*\Z/i)
            {
                my $FSignature = $2;
                if(my $PackageName = getSFormat($CurrentPackage)) {
                    $TypeAttr{"Fields"}{$FName}{"Mangled"} = $PackageName."/".$CurrentClass.".".$FName.":".$FSignature;
                }
            }
            
            $LINE_NP = <OUT>;
            if($Debug) {
                $DContent .= $LINE_NP;
            }
            
            if($LINE_NP=~/flags:/i)
            { # flags: ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_ANNOTATION
                $LINE_NP = <OUT>;
                if($Debug) {
                    $DContent .= $LINE_NP;
                }
            }
            else
            {
                $LINE = $LINE_NP;
                $Stay = 1;
            }
            
            # read the Value
            if($LINE_NP=~/Constant\s*value:\s*([^\s]+)\s(.*?)\s*\Z/i)
            {
              # Java 6: Constant value: ...
              # Java 7: ConstantValue: ...
                my ($TName, $Value) = ($1, $2);
                if($Value)
                {
                    if($Value=~s/Deprecated:\s*true\Z//g) {
                        # deprecated values: ?
                    }
                    $TypeAttr{"Fields"}{$FName}{"Value"} = $Value;
                }
                elsif($TName eq "String") {
                    $TypeAttr{"Fields"}{$FName}{"Value"} = "\@EMPTY_STRING\@";
                }
            }
            else
            {
                $LINE = $LINE_NP;
                $Stay = 1;
            }
        }
        elsif($LINE=~/(\A|\s+)(class|interface)\s+([^\s\{]+)(\s+|\{|\s*\Z)/)
        { # properties of classes and interfaces
            if($TypeAttr{"Name"})
            { # register previous
                %{$TypeInfo{$LVer}{registerType($TypeAttr{"Name"}, $LVer)}} = %TypeAttr;
            }
            
            %TypeAttr = ("Type"=>$2, "Name"=>$3); # reset previous class
            %AnnotationName = (); # reset annotations of the class
            %AnnotationNum = (); # support for Java 7
            $InAnnotations_Class = 1;
            
            $FieldPos = 0; # reset field position
            $CurrentMethod = ""; # reset current method
            $TypeAttr{"Archive"} = $ArchiveName;
            if($TypeAttr{"Name"}=~/\A(.+)\.([^.]+)\Z/)
            {
                $CurrentClass = $2;
                $TypeAttr{"Package"} = $1;
                $CurrentPackage = $TypeAttr{"Package"};
            }
            else
            {
                $CurrentClass = $TypeAttr{"Name"};
                $CurrentPackage = "";
            }
            
            if($CurrentClass=~s/#/./g)
            { # javax.swing.text.GlyphView.GlyphPainter <=> GlyphView$GlyphPainter
                $TypeAttr{"Name"}=~s/#/./g;
            }
            
            $CurrentClass_Short = $CurrentClass;
            if(index($CurrentClass_Short, "<")!=-1) {
                $CurrentClass_Short=~s/<.+>//g;
            }
            
            if($LINE=~/(\A|\s+)(public|protected|private)\s+/) {
                $TypeAttr{"Access"} = $2;
            }
            else {
                $TypeAttr{"Access"} = "package-private";
            }
            if($LINE=~/\s+extends\s+([^\s\{]+)/)
            {
                my $Extended = $1;
                
                if($TypeAttr{"Type"} eq "class")
                {
                    if($Extended ne $CurrentPackage.".".$CurrentClass) {
                        $TypeAttr{"SuperClass"} = registerType($Extended, $LVer);
                    }
                }
                elsif($TypeAttr{"Type"} eq "interface")
                {
                    my @Elems = sepParams($Extended, 0, 0);
                    foreach my $SuperInterface (@Elems)
                    {
                        if($SuperInterface ne $CurrentPackage.".".$CurrentClass) {
                            $TypeAttr{"SuperInterface"}{registerType($SuperInterface, $LVer)} = 1;
                        }
                        
                        if($SuperInterface eq "java.lang.annotation.Annotation") {
                            $TypeAttr{"Annotation"} = 1;
                        }
                    }
                }
            }
            if($LINE=~/\s+implements\s+([^\s\{]+)/)
            {
                my $Implemented = $1;
                my @Elems = sepParams($Implemented, 0, 0);
                
                foreach my $SuperInterface (@Elems) {
                    $TypeAttr{"SuperInterface"}{registerType($SuperInterface, $LVer)} = 1;
                }
            }
            if($LINE=~/(\A|\s+)abstract\s+/) {
                $TypeAttr{"Abstract"} = 1;
            }
            if($LINE=~/(\A|\s+)final\s+/) {
                $TypeAttr{"Final"} = 1;
            }
            if($LINE=~/(\A|\s+)static\s+/) {
                $TypeAttr{"Static"} = 1;
            }
            
            if($TmplP) {
                $TypeAttr{"GenericParam"} = $TmplP;
            }
        }
        elsif(index($LINE, "Deprecated: true")!=-1
        or index($LINE, "Deprecated: length")!=-1)
        { # deprecated method or class
            if($CurrentMethod) {
                $MethodInfo{$LVer}{$MName_Mid{$CurrentMethod}}{"Deprecated"} = 1;
            }
            elsif($CurrentClass) {
                $TypeAttr{"Deprecated"} = 1;
            }
        }
        elsif(index($LINE, "RuntimeInvisibleAnnotations")!=-1
        or index($LINE, "RuntimeVisibleAnnotations")!=-1)
        {
            $InAnnotations = 1;
            $InCode = 0;
        }
        elsif(defined $InAnnotations and index($LINE, "InnerClasses")!=-1) {
            $InAnnotations = undef;
        }
        else
        {
            # unparsed
        }
    }
    
    if($TypeAttr{"Name"})
    { # register last
        %{$TypeInfo{$LVer}{registerType($TypeAttr{"Name"}, $LVer)}} = %TypeAttr;
    }
    
    waitpid($Pid, 0);
    chdir($In::Opt{"OrigDir"});
    
    if(my $Err = $?>>8) {
        exitStatus("Error", "failed to run javap");
    }
    
    if(not $NonEmpty) {
        exitStatus("Error", "internal error in parser");
    }
    
    if($Debug) {
        appendFile(getDebugDir($LVer)."/class-dump.txt", $DContent);
    }
}

sub registerType($$)
{
    my ($TName, $LVer) = @_;
    
    if(not $TName) {
        return 0;
    }
    
    $TName=~s/#/./g;
    if($TName_Tid{$LVer}{$TName}) {
        return $TName_Tid{$LVer}{$TName};
    }
    
    if(not $TName_Tid{$LVer}{$TName})
    {
        my $ID = undef;
        if($In::Opt{"Reproducible"}) {
            $ID = getMd5($TName);
        }
        else {
            $ID = ++$T_ID;
        }
        $TName_Tid{$LVer}{$TName} = "$ID";
    }
    
    my $Tid = $TName_Tid{$LVer}{$TName};
    $TypeInfo{$LVer}{$Tid}{"Name"} = $TName;
    if($TName=~/(.+)\[\]\Z/)
    {
        if(my $BaseTypeId = registerType($1, $LVer))
        {
            $TypeInfo{$LVer}{$Tid}{"BaseType"} = $BaseTypeId;
            $TypeInfo{$LVer}{$Tid}{"Type"} = "array";
        }
    }
    elsif($TName=~/(.+)\.\.\.\Z/)
    {
        if(my $BaseTypeId = registerType($1, $LVer))
        {
            $TypeInfo{$LVer}{$Tid}{"BaseType"} = $BaseTypeId;
            $TypeInfo{$LVer}{$Tid}{"Type"} = "variable-arity";
        }
    }
    
    return $Tid;
}

sub readClasses_Usage($)
{
    my $Paths = $_[0];
    
    my $JavapCmd = getCmdPath("javap");
    if(not $JavapCmd) {
        exitStatus("Not_Found", "can't find \"javap\" command");
    }
    
    my $Input = join(" ", @{$Paths});
    if($In::Opt{"OS"} ne "windows")
    { # on unix ensure that the system does not try and interpret the $, by escaping it
        $Input=~s/\$/\\\$/g;
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    chdir($TmpDir."/".$ExtractCounter);
    open(CONTENT, "$JavapCmd -c -private $Input 2>\"$TmpDir/warn\" |");
    while(<CONTENT>)
    {
        if(/\/\/\s*(Method|InterfaceMethod)\s+(.+)\s*\Z/)
        {
            my $M = $2;
            $In::Opt{"UsedMethods_Client"}{$M} = 1;
            
            if($M=~/\A(.*)+\.\w+\:\(/)
            {
                my $C = $1;
                $C=~s/\//./g;
                $In::Opt{"UsedClasses_Client"}{$C} = 1;
            }
        }
        elsif(/\/\/\s*Field\s+(.+)\s*\Z/)
        {
            # my $FieldName = $1;
            # if(/\s+(putfield|getfield|getstatic|putstatic)\s+/) {
            #     $UsedFields_Client{$FieldName} = $1;
            # }
        }
        elsif(/ ([^\s]+) [^: ]+\(([^()]+)\)/)
        {
            my ($Ret, $Params) = ($1, $2);
            
            $Ret=~s/\[\]//g; # quals
            $In::Opt{"UsedClasses_Client"}{$Ret} = 1;
            
            foreach my $Param (split(/\s*,\s*/, $Params))
            {
                $Param=~s/\[\]//g; # quals
                $In::Opt{"UsedClasses_Client"}{$Param} = 1;
            }
        }
        elsif(/ class /)
        {
            if(/extends ([^\s{]+)/)
            {
                foreach my $Class (split(/\s*,\s*/, $1)) {
                    $In::Opt{"UsedClasses_Client"}{$Class} = 1;
                }
            }
            
            if(/implements ([^\s{]+)/)
            {
                foreach my $Interface (split(/\s*,\s*/, $1)) {
                    $In::Opt{"UsedClasses_Client"}{$Interface} = 1;
                }
            }
        }
    }
    close(CONTENT);
    chdir($In::Opt{"OrigDir"});
}

return 1;

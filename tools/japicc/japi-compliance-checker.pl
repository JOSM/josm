#!/usr/bin/perl
###########################################################################
# Java API Compliance Checker (JAPICC) 2.3
# A tool for checking backward compatibility of a Java library API
#
# Written by Andrey Ponomarenko
#
# Copyright (C) 2011-2017 Andrey Ponomarenko's ABI Laboratory
#
# PLATFORMS
# =========
#  Linux, FreeBSD, Mac OS X, MS Windows
#
# REQUIREMENTS
# ============
#  Linux, FreeBSD, Mac OS X
#    - JDK or OpenJDK - development files (javap, javac)
#    - Perl 5 (5.8 or newer)
#
#  MS Windows
#    - JDK or OpenJDK (javap, javac)
#    - Active Perl 5 (5.8 or newer)
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
use Getopt::Long;
Getopt::Long::Configure ("posix_default", "no_ignore_case", "permute");
use File::Path qw(mkpath rmtree);
use File::Temp qw(tempdir);
use File::Basename qw(dirname);
use Cwd qw(abs_path cwd);
use Data::Dumper;

my $TOOL_VERSION = "2.3";
my $API_DUMP_VERSION = "2.2";
my $API_DUMP_VERSION_MIN = "2.2";

# Internal modules
my $MODULES_DIR = getModules();
push(@INC, dirname($MODULES_DIR));

# Basic modules
my %LoadedModules = ();
loadModule("Basic");
loadModule("Input");
loadModule("Path");
loadModule("Logging");
loadModule("Utils");
loadModule("TypeAttr");
loadModule("Filter");
loadModule("SysFiles");
loadModule("Descriptor");
loadModule("Mangling");

# Rules DB
my %RULES_PATH = (
"Binary" => $MODULES_DIR."/RulesBin.xml",
"Source" => $MODULES_DIR."/RulesSrc.xml");

my $CmdName = getFilename($0);

my %HomePage = (
    "Dev"=>"https://github.com/lvc/japi-compliance-checker",
    "Doc"=>"https://lvc.github.io/japi-compliance-checker/"
);

my $ShortUsage = "Java API Compliance Checker (JAPICC) $TOOL_VERSION
A tool for checking backward compatibility of a Java library API
Copyright (C) 2017 Andrey Ponomarenko's ABI Laboratory
License: GNU LGPL or GNU GPL

Usage: $CmdName [options]
Example: $CmdName OLD.jar NEW.jar

More info: $CmdName --help";

if($#ARGV==-1)
{
    printMsg("INFO", $ShortUsage);
    exit(0);
}

GetOptions("h|help!" => \$In::Opt{"Help"},
  "v|version!" => \$In::Opt{"ShowVersion"},
  "dumpversion!" => \$In::Opt{"DumpVersion"},
# general options
  "l|lib|library=s" => \$In::Opt{"TargetLib"},
  "d1|old|o=s" => \$In::Desc{1}{"Path"},
  "d2|new|n=s" => \$In::Desc{2}{"Path"},
# extra options
  "client|app=s" => \$In::Opt{"ClientPath"},
  "binary|bin!" => \$In::Opt{"BinaryOnly"},
  "source|src!" => \$In::Opt{"SourceOnly"},
  "v1|version1|vnum=s" => \$In::Desc{1}{"TargetVersion"},
  "v2|version2=s" => \$In::Desc{2}{"TargetVersion"},
  "s|strict!" => \$In::Opt{"StrictCompat"},
  "keep-internal!" => \$In::Opt{"KeepInternal"},
  "skip-internal-packages|skip-internal=s" => \$In::Opt{"SkipInternalPackages"},
  "skip-internal-types=s" => \$In::Opt{"SkipInternalTypes"},
  "dump|dump-api=s" => \$In::Opt{"DumpAPI"},
  "check-packages=s" => \$In::Opt{"CheckPackages"},
  "classes-list=s" => \$In::Opt{"ClassListPath"},
  "annotations-list=s" => \$In::Opt{"AnnotationsListPath"},
  "skip-annotations-list=s" => \$In::Opt{"SkipAnnotationsListPath"},
  "skip-deprecated!" => \$In::Opt{"SkipDeprecated"},
  "skip-classes=s" => \$In::Opt{"SkipClassesList"},
  "skip-packages=s" => \$In::Opt{"SkipPackagesList"},
  "short" => \$In::Opt{"ShortMode"},
  "dump-path=s" => \$In::Opt{"OutputDumpPath"},
  "report-path=s" => \$In::Opt{"OutputReportPath"},
  "bin-report-path=s" => \$In::Opt{"BinaryReportPath"},
  "src-report-path=s" => \$In::Opt{"SourceReportPath"},
  "quick!" => \$In::Opt{"Quick"},
  "sort!" => \$In::Opt{"SortDump"},
  "show-access!" => \$In::Opt{"ShowAccess"},
  "limit-affected=s" => \$In::Opt{"AffectLimit"},
  "hide-templates!" => \$In::Opt{"HideTemplates"},
  "show-packages!" => \$In::Opt{"ShowPackages"},
  "compact!" => \$In::Opt{"Compact"},
  "added-annotations!" => \$In::Opt{"AddedAnnotations"},
  "removed-annotations!" => \$In::Opt{"RemovedAnnotations"},
  "count-methods=s" => \$In::Opt{"CountMethods"},
  "dep1=s" => \$In::Desc{1}{"DepDump"},
  "dep2=s" => \$In::Desc{2}{"DepDump"},
  "old-style!" => \$In::Opt{"OldStyle"},
# other options
  "test!" => \$In::Opt{"TestTool"},
  "debug!" => \$In::Opt{"Debug"},
  "title=s" => \$In::Opt{"TargetTitle"},
  "jdk-path=s" => \$In::Opt{"JdkPath"},
  "external-css=s" => \$In::Opt{"ExternCss"},
  "external-js=s" => \$In::Opt{"ExternJs"},
# deprecated
  "minimal!" => \$In::Opt{"Minimal"},
  "hide-packages!" => \$In::Opt{"HidePackages"},
# private
  "all-affected!" => \$In::Opt{"AllAffected"}
) or errMsg();

if(@ARGV)
{ 
    if($#ARGV==1)
    { # japi-compliance-checker OLD.jar NEW.jar
        $In::Desc{1}{"Path"} = $ARGV[0];
        $In::Desc{2}{"Path"} = $ARGV[1];
    }
    else {
        errMsg();
    }
}

sub errMsg()
{
    printMsg("INFO", "\n".$ShortUsage);
    exit(getErrorCode("Error"));
}

my $HelpMessage = "
NAME:
  Java API Compliance Checker ($CmdName)
  Check backward compatibility of a Java library API

DESCRIPTION:
  Java API Compliance Checker (JAPICC) is a tool for checking backward
  binary/source compatibility of a Java library API. The tool checks class
  declarations of old and new versions and analyzes changes that may break
  compatibility: removed class members, added abstract methods, etc.
  
  Break of the binary compatibility may result in crash or incorrect behavior
  of existing clients built with an old library version if they run with a
  new one. Break of the source compatibility may result in recompilation
  errors with a new library version.

  The tool is intended for developers of software libraries and maintainers
  of operating systems who are interested in ensuring backward compatibility,
  i.e. allow old clients to run or to be recompiled with newer library
  versions.

  This tool is free software: you can redistribute it and/or modify it
  under the terms of the GNU LGPL or GNU GPL.

USAGE:
  $CmdName [options]

EXAMPLE 1:
  $CmdName OLD.jar NEW.jar

EXAMPLE 2:
  $CmdName -lib NAME -old OLD.xml -new NEW.xml
  OLD.xml and NEW.xml are XML-descriptors:

    <version>
        1.0
    </version>
    
    <archives>
        /path1/to/JAR(s)/
        /path2/to/JAR(s)/
        ...
    </archives>

INFORMATION OPTIONS:
  -h|-help
      Print this help.

  -v|-version
      Print version information.

  -dumpversion
      Print the tool version ($TOOL_VERSION) and don't do anything else.

GENERAL OPTIONS:
  -l|-library NAME
      Library name (without version).

  -old|-d1 PATH
      Descriptor of the 1st (old) library version.
      It may be one of the following:
      
         1. Java archive (*.jar)
         2. XML-descriptor (VERSION.xml file):

              <version>
                  1.0
              </version>
              
              <archives>
                  /path1/to/JAR(s)/
                  /path2/to/JAR(s)/
                   ...
              </archives>

                 ...
         
         3. API dump generated by -dump option

      If you are using *.jar as a descriptor then you should
      specify version numbers with -v1 and -v2 options too.
      If version numbers are not specified then the tool will
      try to detect them automatically.

  -new|-d2 PATH
      Descriptor of the 2nd (new) library version.

EXTRA OPTIONS:
  -client|-app PATH
      This option allows to specify the client Java archive that should be
      checked for portability to the new library version.

  -binary|-bin
      Show \"Binary\" compatibility problems only.
      Generate report to \"bin_compat_report.html\".

  -source|-src
      Show \"Source\" compatibility problems only.
      Generate report to \"src_compat_report.html\".

  -v1|-version1 NUM
      Specify 1st API version outside the descriptor. This option is needed
      if you have prefered an alternative descriptor type (see -d1 option).
      
      In general case you should specify it in the XML descriptor:
          <version>
              VERSION
          </version>

  -v2|-version2 NUM
      Specify 2nd library version outside the descriptor.

  -vnum NUM
      Specify the library version in the generated API dump.

  -s|-strict
      Treat all API compatibility warnings as problems.

  -keep-internal
      Do not skip checking of these packages:
        *impl*
        *internal*
        *examples*
  
  -skip-internal-packages PATTERN
      Do not check packages matched by the regular expression.
  
  -skip-internal-types PATTERN
      Do not check types (classes and interfaces) matched by the regular
      expression. It's matched against full qualified type names (e.g.
      'org.xyz.Name<T>'). It has to match any part of type name.
  
  -dump|-dump-api PATH
      Dump library API to gzipped TXT format file. You can transfer it
      anywhere and pass instead of the descriptor. Also it may be used
      for debugging the tool. PATH is the path to the Java archive or
      XML descriptor of the library.
  
  -check-packages PATTERN
      Check packages matched by the regular expression. Other packages
      will not be checked.
  
  -classes-list PATH
      This option allows to specify a file with a list
      of classes that should be checked, other classes will not be checked.
  
  -annotations-list PATH
      Specifies a file with a list of annotations. The tool will check only
      classes annotated by the annotations from the list. Other classes
      will not be checked.
  
  -skip-annotations-list PATH
      Skip checking of classes annotated by the annotations in the list.
      
  -skip-deprecated
      Skip analysis of deprecated methods and classes.
      
  -skip-classes PATH
      This option allows to specify a file with a list
      of classes that should not be checked.
      
  -skip-packages PATH
      This option allows to specify a file with a list
      of packages that should not be checked.
      
  -short
      Do not list added/removed methods.
  
  -dump-path PATH
      Specify a *.dump file path where to generate an API dump.
      Default: 
          api_dumps/LIB_NAME/VERSION/API.dump

  -report-path PATH
      Path to compatibility report.
      Default: 
          compat_reports/LIB_NAME/V1_to_V2/compat_report.html

  -bin-report-path PATH
      Path to \"Binary\" compatibility report.
      Default: 
          compat_reports/LIB_NAME/V1_to_V2/bin_compat_report.html

  -src-report-path PATH
      Path to \"Source\" compatibility report.
      Default: 
          compat_reports/LIB_NAME/V1_to_V2/src_compat_report.html

  -quick
      Quick analysis.
      Disabled:
        - analysis of method parameter names
        - analysis of class field values
        - analysis of usage of added abstract methods
        - distinction of deprecated methods and classes

  -sort
      Enable sorting of data in API dumps.
      
  -show-access
      Show access level of non-public methods listed in the report.
      
  -hide-templates
      Hide template parameters in the report.
  
  -hide-packages
  -minimal
      Do nothing.
  
  -show-packages
      Show package names in the report.
      
  -limit-affected LIMIT
      The maximum number of affected methods listed under the description
      of the changed type in the report.
  
  -compact
      Try to simplify formatting and reduce size of the report (for a big
      set of changes).
  
  -added-annotations
      Apply filters by annotations only to new version of the library.
  
  -removed-annotations
      Apply filters by annotations only to old version of the library.
  
  -count-methods PATH
      Count total public methods in the API dump.
  
  -dep1 PATH
  -dep2 PATH
      Path to the API dump of the required dependency archive. It will
      be used to resolve overwritten methods and more.
  
  -old-style
      Generate old-style report.

OTHER OPTIONS:
  -test
      Run internal tests. Create two incompatible versions of a sample library
      and run the tool to check them for compatibility. This option allows to
      check if the tool works correctly in the current environment.

  -debug
      Debugging mode. Print debug info on the screen. Save intermediate
      analysis stages in the debug directory:
          debug/LIB_NAME/VER/

      Also consider using -dump option for debugging the tool.

  -title NAME
      Change library name in the report title to NAME. By default
      will be displayed a name specified by -l option.

  -jdk-path PATH
      Path to the JDK install tree (e.g. /usr/lib/jvm/java-7-openjdk-amd64).

  -external-css PATH
      Generate CSS styles file to PATH. This helps to save space when
      generating thousands of reports.

  -external-js PATH
      Generate JS script file to PATH.

REPORT:
    Compatibility report will be generated to:
        compat_reports/LIB_NAME/V1_to_V2/compat_report.html

EXIT CODES:
    0 - Compatible. The tool has run without any errors.
    non-zero - Incompatible or the tool has run with errors.

MORE INFO:
    ".$HomePage{"Doc"}."
    ".$HomePage{"Dev"};

sub helpMsg() {
    printMsg("INFO", $HelpMessage."\n");
}

#Aliases
my (%MethodInfo, %TypeInfo, %TName_Tid) = ();

my %TName_Tid_Generic = ();

#Separate checked and unchecked exceptions
my %KnownRuntimeExceptions= map {$_=>1} (
    "java.lang.AnnotationTypeMismatchException",
    "java.lang.ArithmeticException",
    "java.lang.ArrayStoreException",
    "java.lang.BufferOverflowException",
    "java.lang.BufferUnderflowException",
    "java.lang.CannotRedoException",
    "java.lang.CannotUndoException",
    "java.lang.ClassCastException",
    "java.lang.CMMException",
    "java.lang.ConcurrentModificationException",
    "java.lang.DataBindingException",
    "java.lang.DOMException",
    "java.lang.EmptyStackException",
    "java.lang.EnumConstantNotPresentException",
    "java.lang.EventException",
    "java.lang.IllegalArgumentException",
    "java.lang.IllegalMonitorStateException",
    "java.lang.IllegalPathStateException",
    "java.lang.IllegalStateException",
    "java.lang.ImagingOpException",
    "java.lang.IncompleteAnnotationException",
    "java.lang.IndexOutOfBoundsException",
    "java.lang.JMRuntimeException",
    "java.lang.LSException",
    "java.lang.MalformedParameterizedTypeException",
    "java.lang.MirroredTypeException",
    "java.lang.MirroredTypesException",
    "java.lang.MissingResourceException",
    "java.lang.NegativeArraySizeException",
    "java.lang.NoSuchElementException",
    "java.lang.NoSuchMechanismException",
    "java.lang.NullPointerException",
    "java.lang.ProfileDataException",
    "java.lang.ProviderException",
    "java.lang.RasterFormatException",
    "java.lang.RejectedExecutionException",
    "java.lang.SecurityException",
    "java.lang.SystemException",
    "java.lang.TypeConstraintException",
    "java.lang.TypeNotPresentException",
    "java.lang.UndeclaredThrowableException",
    "java.lang.UnknownAnnotationValueException",
    "java.lang.UnknownElementException",
    "java.lang.UnknownEntityException",
    "java.lang.UnknownTypeException",
    "java.lang.UnmodifiableSetException",
    "java.lang.UnsupportedOperationException",
    "java.lang.WebServiceException",
    "java.lang.WrongMethodTypeException"
);

#java.lang.Object
my %JavaObjectMethod = (
    
    "java/lang/Object.clone:()Ljava/lang/Object;" => 1,
    "java/lang/Object.equals:(Ljava/lang/Object;)Z" => 1,
    "java/lang/Object.finalize:()V" => 1,
    "java/lang/Object.getClass:()Ljava/lang/Class;" => 1,
    "java/lang/Object.hashCode:()I" => 1,
    "java/lang/Object.notify:()V" => 1,
    "java/lang/Object.notifyAll:()V" => 1,
    "java/lang/Object.toString:()Ljava/lang/String;" => 1,
    "java/lang/Object.wait:()V" => 1,
    "java/lang/Object.wait:(J)V" => 1,
    "java/lang/Object.wait:(JI)V" => 1
);

#Global variables
my %Cache;
my %RESULT;
my $TOP_REF = "<a class='top_ref' href='#Top'>to the top</a>";

#Types
my %CheckedTypes;

#Classes
my %LibArchives;
my %Class_Methods;
my %Class_Methods_Generic;
my %Class_AbstractMethods;
my %Class_Fields;
my %ClassMethod_AddedUsed;
my %Class_Constructed;

#Methods
my %CheckedMethods;
my %MethodUsed;
my %OldMethodSignature;

#Merging
my %AddedMethod_Abstract;
my %RemovedMethod_Abstract;
my %ChangedReturnFromVoid;
my %CompatRules;
my %IncompleteRules;
my %UnknownRules;

#Report
my %TypeChanges;

#Recursion locks
my @RecurTypes;

#Problem descriptions
my %CompatProblems;
my %TotalAffected;

#Speedup
my %TypeProblemsIndex;

#Rerort
my $ContentID = 1;
my $ContentSpanStart = "<span class=\"section\" onclick=\"sC(this, 'CONTENT_ID')\">\n";
my $ContentSpanStart_Affected = "<span class=\"sect_aff\" onclick=\"sC(this, 'CONTENT_ID')\">\n";
my $ContentSpanEnd = "</span>\n";
my $ContentDivStart = "<div id=\"CONTENT_ID\" style=\"display:none;\">\n";
my $ContentDivEnd = "</div>\n";
my $Content_Counter = 0;

sub getModules()
{
    my $TOOL_DIR = dirname($0);
    if(not $TOOL_DIR)
    { # patch for MS Windows
        $TOOL_DIR = ".";
    }
    my @SEARCH_DIRS = (
        # tool's directory
        abs_path($TOOL_DIR),
        # relative path to modules
        abs_path($TOOL_DIR)."/../share/japi-compliance-checker",
        # install path
        'MODULES_INSTALL_PATH'
    );
    foreach my $DIR (@SEARCH_DIRS)
    {
        if($DIR!~/\A(\/|\w+:[\/\\])/)
        { # relative path
            $DIR = abs_path($TOOL_DIR)."/".$DIR;
        }
        if(-d $DIR."/modules") {
            return $DIR."/modules";
        }
    }
    
    print STDERR "ERROR: can't find modules (Did you installed the tool by 'make install' command?)\n";
    exit(9); # Module_Error
}

sub loadModule($)
{
    my $Name = $_[0];
    if(defined $LoadedModules{$Name}) {
        return;
    }
    my $Path = $MODULES_DIR."/Internals/$Name.pm";
    if(not -f $Path)
    {
        print STDERR "can't access \'$Path\'\n";
        exit(2);
    }
    require $Path;
    $LoadedModules{$Name} = 1;
}

sub readModule($$)
{
    my ($Module, $Name) = @_;
    my $Path = $MODULES_DIR."/Internals/$Module/".$Name;
    if(not -f $Path) {
        exitStatus("Module_Error", "can't access \'$Path\'");
    }
    return readFile($Path);
}

sub mergeClasses()
{
    my %ReportedRemoved = undef;
    
    foreach my $ClassName (keys(%{$Class_Methods{1}}))
    {
        next if(not $ClassName);
        my $Type1_Id = $TName_Tid{1}{$ClassName};
        my $Type1 = getType($Type1_Id, 1);
        
        if($Type1->{"Type"}!~/class|interface/) {
            next;
        }
        
        if(defined $Type1->{"Access"}
        and $Type1->{"Access"}=~/private/) {
            next;
        }
        
        if(not classFilter($Type1, 1, 1)) {
            next;
        }
        
        my $GenericName = getGeneric($ClassName);
        my $Type2_Id = undef;
        
        if(defined $TName_Tid{2}{$ClassName}) {
            $Type2_Id = $TName_Tid{2}{$ClassName};
        }
        elsif(defined $TName_Tid_Generic{2}{$GenericName}) {
            $Type2_Id = $TName_Tid_Generic{2}{$GenericName};
        }
        
        if($Type2_Id)
        {
            my $TName1 = $Type1->{"Name"};
            my $TName2 = getTypeName($Type2_Id, 2);
            
            my $Generic1 = (index($TName1, "<")!=-1);
            my $Generic2 = (index($TName2, "<")!=-1);
            
            if($Generic1 ne $Generic2)
            { # removed generic parameters
                foreach my $Method (keys(%{$Class_Methods{1}{$ClassName}}))
                {
                    if(not methodFilter($Method, 1)) {
                        next;
                    }
                    
                    $CheckedTypes{$ClassName} = 1;
                    $CheckedMethods{$Method} = 1;
                    
                    if($Type1->{"Type"} eq "class")
                    {
                        if($Generic1)
                        {
                            %{$CompatProblems{$Method}{"Class_Became_Raw"}{"this"}} = (
                                "Type_Name"=>$ClassName,
                                "New_Value"=>$TName2,
                                "Target"=>$ClassName);
                        }
                        else
                        {
                            %{$CompatProblems{$Method}{"Class_Became_Generic"}{"this"}} = (
                                "Type_Name"=>$ClassName,
                                "New_Value"=>$TName2,
                                "Target"=>$ClassName);
                        }
                    }
                    else
                    {
                        if($Generic1)
                        {
                            %{$CompatProblems{$Method}{"Interface_Became_Raw"}{"this"}} = (
                                "Type_Name"=>$ClassName,
                                "New_Value"=>$TName2,
                                "Target"=>$ClassName);
                        }
                        else
                        {
                            %{$CompatProblems{$Method}{"Interface_Became_Generic"}{"this"}} = (
                                "Type_Name"=>$ClassName,
                                "New_Value"=>$TName2,
                                "Target"=>$ClassName);
                        }
                    }
                }
            }
        }
        else
        { # classes and interfaces with public methods
            foreach my $Method (keys(%{$Class_Methods{1}{$ClassName}}))
            {
                if(not methodFilter($Method, 1)) {
                    next;
                }
                
                $CheckedTypes{$ClassName} = 1;
                $CheckedMethods{$Method} = 1;
                
                if($Type1->{"Type"} eq "class")
                {
                    %{$CompatProblems{$Method}{"Removed_Class"}{"this"}} = (
                        "Type_Name"=>$ClassName,
                        "Target"=>$ClassName);
                }
                else
                {
                    %{$CompatProblems{$Method}{"Removed_Interface"}{"this"}} = (
                        "Type_Name"=>$ClassName,
                        "Target"=>$ClassName);
                }
                
                $ReportedRemoved{$ClassName} = 1;
            }
        }
    }
    
    foreach my $Class_Id (keys(%{$TypeInfo{1}}))
    {
        my $Class1 = getType($Class_Id, 1);
        
        if($Class1->{"Type"}!~/class|interface/) {
            next;
        }
        
        if(defined $Class1->{"Access"}
        and $Class1->{"Access"}=~/private/) {
            next;
        }
        
        if(not classFilter($Class1, 1, 1)) {
            next;
        }
        
        my $ClassName = $Class1->{"Name"};
        my $GenericName = getGeneric($ClassName);
        my $Class2_Id = undef;
        
        if(defined $TName_Tid{2}{$ClassName}) {
            $Class2_Id = $TName_Tid{2}{$ClassName};
        }
        elsif(defined $TName_Tid_Generic{2}{$GenericName}) {
            $Class2_Id = $TName_Tid_Generic{2}{$GenericName};
        }
        
        if($Class2_Id)
        { # classes and interfaces with public static fields
            if(not defined $Class_Methods{1}{$ClassName})
            {
                my $Class2 = getType($Class2_Id, 2);
                
                foreach my $Field (keys(%{$Class1->{"Fields"}}))
                {
                    my $FieldInfo = $Class1->{"Fields"}{$Field};
                    
                    my $FAccess = $FieldInfo->{"Access"};
                    if($FAccess=~/private/) {
                        next;
                    }
                    
                    if($FieldInfo->{"Static"})
                    {
                        $CheckedTypes{$ClassName} = 1;
                        
                        if(not defined $Class2->{"Fields"}{$Field})
                        {
                            %{$CompatProblems{".client_method"}{"Removed_NonConstant_Field"}{$Field}}=(
                                "Target"=>$Field,
                                "Type_Name"=>$ClassName,
                                "Type_Type"=>$Class1->{"Type"},
                                "Field_Type"=>getTypeName($FieldInfo->{"Type"}, 1));
                        }
                    }
                }
            }
        }
        else
        { # removed
            if(defined $Class1->{"Annotation"})
            {
                %{$CompatProblems{".client_method"}{"Removed_Annotation"}{"this"}} = (
                    "Type_Name"=>$ClassName,
                    "Target"=>$ClassName);
            }
            
            if(not defined $Class_Methods{1}{$ClassName})
            {
                # classes and interfaces with public static fields
                if(not defined $ReportedRemoved{$ClassName})
                {
                    foreach my $Field (keys(%{$Class1->{"Fields"}}))
                    {
                        my $FieldInfo = $Class1->{"Fields"}{$Field};
                        
                        my $FAccess = $FieldInfo->{"Access"};
                        if($FAccess=~/private/) {
                            next;
                        }
                        
                        if($FieldInfo->{"Static"})
                        {
                            $CheckedTypes{$ClassName} = 1;
                            
                            if($Class1->{"Type"} eq "class")
                            {
                                %{$CompatProblems{".client_method"}{"Removed_Class"}{"this"}} = (
                                    "Type_Name"=>$ClassName,
                                    "Target"=>$ClassName);
                            }
                            else
                            {
                                %{$CompatProblems{".client_method"}{"Removed_Interface"}{"this"}} = (
                                    "Type_Name"=>$ClassName,
                                    "Target"=>$ClassName);
                            }
                        }
                    }
                }
            }
        }
    }
}

sub findFieldPair($$)
{
    my ($Field_Pos, $Pair_Type) = @_;
    foreach my $Pair_Name (sort keys(%{$Pair_Type->{"Fields"}}))
    {
        if(defined $Pair_Type->{"Fields"}{$Pair_Name})
        {
            if($Pair_Type->{"Fields"}{$Pair_Name}{"Pos"} eq $Field_Pos) {
                return $Pair_Name;
            }
        }
    }
    return "lost";
}

my %Severity_Val=(
    "High"=>3,
    "Medium"=>2,
    "Low"=>1,
    "Safe"=>-1
);

sub isRecurType($$)
{
    foreach (@RecurTypes)
    {
        if($_->{"Tid1"} eq $_[0]
        and $_->{"Tid2"} eq $_[1])
        {
            return 1;
        }
    }
    return 0;
}

sub pushType($$)
{
    my %TypeDescriptor = (
        "Tid1"  => $_[0],
        "Tid2"  => $_[1]);
    push(@RecurTypes, \%TypeDescriptor);
}

sub getSFormat($)
{
    my $Name = $_[0];
    $Name=~s/\./\//g;
    return $Name;
}

sub getConstantValue($$)
{
    my ($Value, $ValueType) = @_;
    
    if(not defined $Value) {
        return undef;
    }
    
    if($Value eq "\@EMPTY_STRING\@") {
        return "\"\"";
    }
    elsif($ValueType eq "java.lang.String") {
        return "\"".$Value."\"";
    }
    
    return $Value;
}

sub getInvoked($)
{
    my $TName = $_[0];
    
    if(my @Invoked = sort keys(%{$ClassMethod_AddedUsed{$TName}}))
    {
        my $MFirst = $Invoked[0];
        my $MSignature = unmangle($MFirst);
        $MSignature=~s/\A.+\.(\w+\()/$1/g; # short name
        my $InvokedBy = $ClassMethod_AddedUsed{$TName}{$MFirst};
        return ($MSignature, $InvokedBy);
    }
    
    return ();
}

sub mergeTypes($$)
{
    my ($Type1_Id, $Type2_Id) = @_;
    return {} if(not $Type1_Id or not $Type2_Id);
    
    if(defined $Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id})
    { # already merged
        return $Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id};
    }
    
    if(isRecurType($Type1_Id, $Type2_Id))
    { # do not follow to recursive declarations
        return {};
    }
    
    my %Type1 = %{getType($Type1_Id, 1)};
    my %Type2 = %{getType($Type2_Id, 2)};
    
    return {} if(not $Type1{"Name"} or not $Type2{"Name"});
    return {} if(not $Type1{"Archive"} or not $Type2{"Archive"});
    if($Type1{"Name"} ne $Type2{"Name"})
    {
        if(getGeneric($Type1{"Name"}) ne getGeneric($Type2{"Name"}))
        { # compare type declarations if became generic or raw
            return {};
        }
    }
    
    if(not classFilter(\%Type1, 1, 1)) {
        return {};
    }
    
    $CheckedTypes{$Type1{"Name"}} = 1;
    
    my %SubProblems = ();
    
    if($Type1{"BaseType"} and $Type2{"BaseType"})
    { # check base type (arrays)
        return mergeTypes($Type1{"BaseType"}, $Type2{"BaseType"});
    }
    
    if($Type2{"Type"}!~/(class|interface)/) {
        return {};
    }
    
    if($Type1{"Type"} eq "class" and not $Class_Constructed{1}{$Type1_Id})
    { # class cannot be constructed or inherited by clients
        return {};
    }
    
    if($Type1{"Type"} eq "class"
    and $Type2{"Type"} eq "interface")
    {
        %{$SubProblems{"Class_Became_Interface"}{""}}=(
            "Type_Name"=>$Type1{"Name"});
        
        return ($Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id} = \%SubProblems);
    }
    if($Type1{"Type"} eq "interface"
    and $Type2{"Type"} eq "class")
    {
        %{$SubProblems{"Interface_Became_Class"}{""}}=(
            "Type_Name"=>$Type1{"Name"});
        
        return ($Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id} = \%SubProblems);
    }
    if(not $Type1{"Final"}
    and $Type2{"Final"})
    {
        %{$SubProblems{"Class_Became_Final"}{""}}=(
            "Type_Name"=>$Type1{"Name"},
            "Target"=>$Type1{"Name"});
    }
    if(not $Type1{"Abstract"}
    and $Type2{"Abstract"})
    {
        %{$SubProblems{"Class_Became_Abstract"}{""}}=(
            "Type_Name"=>$Type1{"Name"});
    }
    
    pushType($Type1_Id, $Type2_Id);
    
    foreach my $AddedMethod (keys(%{$AddedMethod_Abstract{$Type2{"Name"}}}))
    {
        if($Type1{"Type"} eq "class")
        {
            if($Type1{"Abstract"})
            {
                if(my @InvokedBy = sort keys(%{$MethodUsed{2}{$AddedMethod}}))
                {
                    %{$SubProblems{"Abstract_Class_Added_Abstract_Method_Invoked_By_Others"}{getSFormat($AddedMethod)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>$Type1{"Type"},
                        "Target"=>$AddedMethod,
                        "Invoked_By"=>$InvokedBy[0]);
                }
                else
                {
                    %{$SubProblems{"Abstract_Class_Added_Abstract_Method"}{getSFormat($AddedMethod)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>$Type1{"Type"},
                        "Target"=>$AddedMethod);
                }
            }
            else
            {
                %{$SubProblems{"NonAbstract_Class_Added_Abstract_Method"}{getSFormat($AddedMethod)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>$Type1{"Type"},
                    "Target"=>$AddedMethod);
            }
        }
        else
        {
            if(my @InvokedBy = sort keys(%{$MethodUsed{2}{$AddedMethod}}))
            {
                %{$SubProblems{"Interface_Added_Abstract_Method_Invoked_By_Others"}{getSFormat($AddedMethod)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>$Type1{"Type"},
                    "Target"=>$AddedMethod,
                    "Invoked_By"=>$InvokedBy[0]);
            }
            else
            {
                %{$SubProblems{"Interface_Added_Abstract_Method"}{getSFormat($AddedMethod)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>$Type1{"Type"},
                    "Target"=>$AddedMethod);
            }
        }
    }
    foreach my $RemovedMethod (keys(%{$RemovedMethod_Abstract{$Type1{"Name"}}}))
    {
        if($Type1{"Type"} eq "class")
        {
            %{$SubProblems{"Class_Removed_Abstract_Method"}{getSFormat($RemovedMethod)}} = (
                "Type_Name"=>$Type1{"Name"},
                "Type_Type"=>$Type1{"Type"},
                "Target"=>$RemovedMethod);
        }
        else
        {
            %{$SubProblems{"Interface_Removed_Abstract_Method"}{getSFormat($RemovedMethod)}} = (
                "Type_Name"=>$Type1{"Name"},
                "Type_Type"=>$Type1{"Type"},
                "Target"=>$RemovedMethod);
        }
    }
    if($Type1{"Type"} eq "class"
    and $Type2{"Type"} eq "class")
    {
        my $SuperClass1 = getType($Type1{"SuperClass"}, 1);
        my $SuperClass2 = getType($Type2{"SuperClass"}, 2);
        
        my $SuperClassName1 = $SuperClass1->{"Name"};
        my $SuperClassName2 = $SuperClass2->{"Name"};
        
        # Java 6: java.lang.Object
        # Java 7: none
        if(not $SuperClassName1) {
            $SuperClassName1 = "java.lang.Object";
        }
        
        if(not $SuperClassName2) {
            $SuperClassName2 = "java.lang.Object";
        }
        
        if($SuperClassName2 ne $SuperClassName1)
        {
            if($SuperClassName1 eq "java.lang.Object")
            {
                if($SuperClass2->{"Abstract"}
                and $Type1{"Abstract"} and $Type2{"Abstract"}
                and keys(%{$Class_AbstractMethods{2}{$SuperClassName2}}))
                {
                    if(my ($Invoked, $InvokedBy) = getInvoked($Type1{"Name"}))
                    {
                        %{$SubProblems{"Abstract_Class_Added_Super_Abstract_Class_Invoked_By_Others"}{""}} = (
                            "Type_Name"=>$Type1{"Name"},
                            "Target"=>$SuperClassName2,
                            "Invoked"=>$Invoked,
                            "Invoked_By"=>$InvokedBy);
                    }
                    else
                    {
                        %{$SubProblems{"Abstract_Class_Added_Super_Abstract_Class"}{""}} = (
                            "Type_Name"=>$Type1{"Name"},
                            "Target"=>$SuperClassName2);
                    }
                }
                else
                {
                    %{$SubProblems{"Added_Super_Class"}{""}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Target"=>$SuperClassName2);
                }
            }
            elsif($SuperClassName2 eq "java.lang.Object")
            {
                %{$SubProblems{"Removed_Super_Class"}{""}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Target"=>$SuperClassName1);
            }
            else
            {
                %{$SubProblems{"Changed_Super_Class"}{""}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Target"=>$SuperClassName1,
                    "Old_Value"=>$SuperClassName1,
                    "New_Value"=>$SuperClassName2);
            }
        }
    }
    my %SuperInterfaces_Old = map {getTypeName($_, 1) => 1} keys(%{$Type1{"SuperInterface"}});
    my %SuperInterfaces_New = map {getTypeName($_, 2) => 1} keys(%{$Type2{"SuperInterface"}});
    foreach my $SuperInterface (keys(%SuperInterfaces_New))
    {
        if(not $SuperInterfaces_Old{$SuperInterface})
        {
            my $HaveMethods = keys(%{$Class_AbstractMethods{2}{$SuperInterface}});
            my $HaveFields = keys(%{$Class_Fields{2}{$SuperInterface}});
            
            if($Type1{"Type"} eq "interface")
            {
                if($HaveMethods
                or $SuperInterface=~/\Ajava\./)
                {
                    if($HaveMethods and checkDefaultImpl(2, $SuperInterface, $Type2{"Name"}))
                    {
                        %{$SubProblems{"Interface_Added_Super_Interface_With_Implemented_Methods"}{getSFormat($SuperInterface)}} = (
                            "Type_Name"=>$Type1{"Name"},
                            "Target"=>$SuperInterface);
                    }
                    else
                    {
                        if(my ($Invoked, $InvokedBy) = getInvoked($Type1{"Name"}))
                        {
                            %{$SubProblems{"Interface_Added_Super_Interface_Used_By_Others"}{getSFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface,
                                "Invoked"=>$Invoked,
                                "Invoked_By"=>$InvokedBy);
                        }
                        else
                        {
                            %{$SubProblems{"Interface_Added_Super_Interface"}{getSFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface);
                        }
                    }
                }
                elsif($HaveFields)
                {
                    %{$SubProblems{"Interface_Added_Super_Constant_Interface"}{getSFormat($SuperInterface)}} = (
                        "Type_Name"=>$Type2{"Name"},
                        "Target"=>$SuperInterface);
                }
                else
                {
                    # empty interface
                }
            }
            else
            {
                if($Type1{"Abstract"} and $Type2{"Abstract"})
                {
                    if($HaveMethods and checkDefaultImpl(2, $SuperInterface, $Type2{"Name"}))
                    {
                        %{$SubProblems{"Abstract_Class_Added_Super_Interface_With_Implemented_Methods"}{getSFormat($SuperInterface)}} = (
                            "Type_Name"=>$Type1{"Name"},
                            "Target"=>$SuperInterface);
                    }
                    else
                    {
                        if(my ($Invoked, $InvokedBy) = getInvoked($Type1{"Name"}))
                        {
                            %{$SubProblems{"Abstract_Class_Added_Super_Interface_Invoked_By_Others"}{getSFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface,
                                "Invoked"=>$Invoked,
                                "Invoked_By"=>$InvokedBy);
                        }
                        else
                        {
                            %{$SubProblems{"Abstract_Class_Added_Super_Interface"}{getSFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface);
                        }
                    }
                }
            }
        }
    }
    foreach my $SuperInterface (keys(%SuperInterfaces_Old))
    {
        if(not $SuperInterfaces_New{$SuperInterface})
        {
            my $HaveMethods = keys(%{$Class_AbstractMethods{1}{$SuperInterface}});
            my $HaveFields = keys(%{$Class_Fields{1}{$SuperInterface}});
            
            if($Type1{"Type"} eq "interface")
            {
                if($HaveMethods
                or $SuperInterface=~/\Ajava\./)
                {
                    %{$SubProblems{"Interface_Removed_Super_Interface"}{getSFormat($SuperInterface)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>"interface",
                        "Target"=>$SuperInterface);
                }
                elsif($HaveFields)
                {
                    %{$SubProblems{"Interface_Removed_Super_Constant_Interface"}{getSFormat($SuperInterface)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Target"=>$SuperInterface);
                }
                else {
                    # empty interface
                }
            }
            else
            {
                %{$SubProblems{"Class_Removed_Super_Interface"}{getSFormat($SuperInterface)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>"class",
                    "Target"=>$SuperInterface);
            }
        }
    }
    
    foreach my $Field_Name (sort keys(%{$Type1{"Fields"}}))
    {# check older fields
        my $Access1 = $Type1{"Fields"}{$Field_Name}{"Access"};
        if($Access1=~/private/) {
            next;
        }
        
        my $Field_Pos1 = $Type1{"Fields"}{$Field_Name}{"Pos"};
        my $FieldType1_Id = $Type1{"Fields"}{$Field_Name}{"Type"};
        my $FieldType1_Name = getTypeName($FieldType1_Id, 1);
        
        if(not $Type2{"Fields"}{$Field_Name})
        {# removed fields
            my $StraightPair_Name = findFieldPair($Field_Pos1, \%Type2);
            if($StraightPair_Name ne "lost" and not $Type1{"Fields"}{$StraightPair_Name}
            and $FieldType1_Name eq getTypeName($Type2{"Fields"}{$StraightPair_Name}{"Type"}, 2))
            {
                if(my $Constant = getConstantValue($Type1{"Fields"}{$Field_Name}{"Value"}, $FieldType1_Name))
                {
                    %{$SubProblems{"Renamed_Constant_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Old_Value"=>$Field_Name,
                        "New_Value"=>$StraightPair_Name,
                        "Field_Type"=>$FieldType1_Name,
                        "Field_Value"=>$Constant);
                }
                else
                {
                    %{$SubProblems{"Renamed_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Old_Value"=>$Field_Name,
                        "New_Value"=>$StraightPair_Name,
                        "Field_Type"=>$FieldType1_Name);
                }
            }
            else
            {
                if(my $Constant = getConstantValue($Type1{"Fields"}{$Field_Name}{"Value"}, $FieldType1_Name))
                { # has a compile-time constant value
                    %{$SubProblems{"Removed_Constant_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Field_Value"=>$Constant,
                        "Field_Type"=>$FieldType1_Name,
                        "Type_Type"=>$Type1{"Type"});
                }
                else
                {
                    %{$SubProblems{"Removed_NonConstant_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>$Type1{"Type"},
                        "Field_Type"=>$FieldType1_Name);
                }
            }
            next;
        }
        
        my $FieldType2_Id = $Type2{"Fields"}{$Field_Name}{"Type"};
        my $FieldType2_Name = getTypeName($FieldType2_Id, 2);
        
        if(not $Type1{"Fields"}{$Field_Name}{"Static"}
        and $Type2{"Fields"}{$Field_Name}{"Static"})
        {
            if(not $Type1{"Fields"}{$Field_Name}{"Value"})
            {
                %{$SubProblems{"NonConstant_Field_Became_Static"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Field_Type"=>$FieldType1_Name,
                    "Type_Name"=>$Type1{"Name"});
            }
        }
        elsif($Type1{"Fields"}{$Field_Name}{"Static"}
        and not $Type2{"Fields"}{$Field_Name}{"Static"})
        {
            if($Type1{"Fields"}{$Field_Name}{"Value"})
            {
                %{$SubProblems{"Constant_Field_Became_NonStatic"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Field_Type"=>$FieldType1_Name,
                    "Type_Name"=>$Type1{"Name"});
            }
            else
            {
                %{$SubProblems{"NonConstant_Field_Became_NonStatic"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Field_Type"=>$FieldType1_Name,
                    "Type_Name"=>$Type1{"Name"});
            }
        }
        if(not $Type1{"Fields"}{$Field_Name}{"Final"}
        and $Type2{"Fields"}{$Field_Name}{"Final"})
        {
            %{$SubProblems{"Field_Became_Final"}{$Field_Name}}=(
                "Target"=>$Field_Name,
                "Field_Type"=>$FieldType1_Name,
                "Type_Name"=>$Type1{"Name"});
        }
        elsif($Type1{"Fields"}{$Field_Name}{"Final"}
        and not $Type2{"Fields"}{$Field_Name}{"Final"})
        {
            %{$SubProblems{"Field_Became_NonFinal"}{$Field_Name}}=(
                "Target"=>$Field_Name,
                "Field_Type"=>$FieldType1_Name,
                "Type_Name"=>$Type1{"Name"});
        }
        my $Access2 = $Type2{"Fields"}{$Field_Name}{"Access"};
        if($Access1 eq "public" and $Access2=~/protected|private/
        or $Access1 eq "protected" and $Access2=~/private/)
        {
            if($Access2 eq "package-private")
            {
                %{$SubProblems{"Changed_Field_Access_To_Package_Private"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Type_Name"=>$Type1{"Name"},
                    "Old_Value"=>$Access1,
                    "New_Value"=>$Access2);
            }
            else
            {
                %{$SubProblems{"Changed_Field_Access"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Type_Name"=>$Type1{"Name"},
                    "Old_Value"=>$Access1,
                    "New_Value"=>$Access2);
            }
        }
        
        my $Value1 = getConstantValue($Type1{"Fields"}{$Field_Name}{"Value"}, $FieldType1_Name);
        my $Value2 = getConstantValue($Type2{"Fields"}{$Field_Name}{"Value"}, $FieldType2_Name);
        
        if($Value1 ne $Value2)
        {
            if($Value1 and $Value2)
            {
                if($Type1{"Fields"}{$Field_Name}{"Final"}
                and $Type2{"Fields"}{$Field_Name}{"Final"})
                {
                    if($Field_Name=~/(\A|_)(VERSION|VERNUM|BUILDNUMBER|BUILD)(_|\Z)/i)
                    {
                        %{$SubProblems{"Changed_Final_Version_Field_Value"}{$Field_Name}}=(
                            "Target"=>$Field_Name,
                            "Field_Type"=>$FieldType1_Name,
                            "Type_Name"=>$Type1{"Name"},
                            "Old_Value"=>$Value1,
                            "New_Value"=>$Value2);
                    }
                    else
                    {
                        %{$SubProblems{"Changed_Final_Field_Value"}{$Field_Name}}=(
                            "Target"=>$Field_Name,
                            "Field_Type"=>$FieldType1_Name,
                            "Type_Name"=>$Type1{"Name"},
                            "Old_Value"=>$Value1,
                            "New_Value"=>$Value2);
                    }
                }
            }
        }
        
        my %Sub_SubChanges = detectTypeChange(\%Type1, \%Type2, $FieldType1_Id, $FieldType2_Id, "Field");
        foreach my $Sub_SubProblemType (keys(%Sub_SubChanges))
        {
            %{$SubProblems{$Sub_SubProblemType}{$Field_Name}}=(
                "Target"=>$Field_Name,
                "Type_Name"=>$Type1{"Name"});
            
            foreach my $Attr (keys(%{$Sub_SubChanges{$Sub_SubProblemType}}))
            {
                $SubProblems{$Sub_SubProblemType}{$Field_Name}{$Attr} = $Sub_SubChanges{$Sub_SubProblemType}{$Attr};
            }
        }
        
        if($FieldType1_Id and $FieldType2_Id)
        { # check field type change
            my $Sub_SubProblems = mergeTypes($FieldType1_Id, $FieldType2_Id);
            my %DupProblems = ();
            
            foreach my $Sub_SubProblemType (sort keys(%{$Sub_SubProblems}))
            {
                foreach my $Sub_SubLocation (sort {length($a)<=>length($b)} sort keys(%{$Sub_SubProblems->{$Sub_SubProblemType}}))
                {
                    if(not defined $In::Opt{"AllAffected"})
                    {
                        if(defined $DupProblems{$Sub_SubProblems->{$Sub_SubProblemType}{$Sub_SubLocation}}) {
                            next;
                        }
                    }
                    
                    my $NewLocation = ($Sub_SubLocation)?$Field_Name.".".$Sub_SubLocation:$Field_Name;
                    $SubProblems{$Sub_SubProblemType}{$NewLocation} = $Sub_SubProblems->{$Sub_SubProblemType}{$Sub_SubLocation};
                    
                    if(not defined $In::Opt{"AllAffected"})
                    {
                        $DupProblems{$Sub_SubProblems->{$Sub_SubProblemType}{$Sub_SubLocation}} = 1;
                    }
                }
            }
            %DupProblems = ();
        }
    }
    
    foreach my $Field_Name (sort keys(%{$Type2{"Fields"}}))
    { # check added fields
        if($Type2{"Fields"}{$Field_Name}{"Access"}=~/private/) {
            next;
        }
        my $FieldPos2 = $Type2{"Fields"}{$Field_Name}{"Pos"};
        my $FieldType2_Id = $Type2{"Fields"}{$Field_Name}{"Type"};
        my $FieldType2_Name = getTypeName($FieldType2_Id, 2);
        
        if(not $Type1{"Fields"}{$Field_Name})
        {# added fields
            my $StraightPair_Name = findFieldPair($FieldPos2, \%Type1);
            if($StraightPair_Name ne "lost" and not $Type2{"Fields"}{$StraightPair_Name}
            and getTypeName($Type1{"Fields"}{$StraightPair_Name}{"Type"}, 1) eq $FieldType2_Name)
            {
                # Already reported as "Renamed_Field" or "Renamed_Constant_Field"
            }
            else
            {
                if($Type1{"Type"} eq "interface")
                {
                    %{$SubProblems{"Interface_Added_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"});
                }
                else
                {
                    %{$SubProblems{"Class_Added_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"});
                }
            }
        }
    }
    
    pop(@RecurTypes);
    return ($Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id} = \%SubProblems);
}

sub checkDefaultImpl($$$)
{ # Check if all abstract methods of the super class have
  # default implementations in the class
    my ($LVer, $SuperClassName, $ClassName) = @_;
    
    foreach my $Method (keys(%{$Class_AbstractMethods{$LVer}{$SuperClassName}}))
    {
        if(my $Overridden = findMethod_Class($Method, $ClassName, $LVer))
        {
            if($MethodInfo{$LVer}{$Overridden}{"Abstract"}) {
                return 0;
            }
        }
        else {
            return 0;
        }
    }
    
    return 1;
}

sub getMSuffix($)
{
    my $Method = $_[0];
    if($Method=~/(\(.*\))/) {
        return $1;
    }
    return "";
}

sub getMShort($)
{
    my $Method = $_[0];
    if($Method=~/([^\.]+)\:\(/) {
        return $1;
    }
    return "";
}

sub findMethod($$$$)
{
    my ($Method, $MethodVersion, $ClassName, $ClassVersion) = @_;
    
    my $GenericName = getGeneric($ClassName);
    my $ClassId = undef;
    
    if(defined $TName_Tid{$ClassVersion}{$ClassName}) {
        $ClassId = $TName_Tid{$ClassVersion}{$ClassName};
    }
    elsif(defined $TName_Tid_Generic{$ClassVersion}{$GenericName}) {
        $ClassId = $TName_Tid_Generic{$ClassVersion}{$GenericName};
    }
    
    if($ClassId)
    {
        my @Search = ();
        if(getTypeType($ClassId, $ClassVersion) eq "class")
        {
            if(my $SuperClassId = $TypeInfo{$ClassVersion}{$ClassId}{"SuperClass"}) {
                push(@Search, $SuperClassId);
            }
        }
        
        if(not defined $MethodInfo{$MethodVersion}{$Method}
        or $MethodInfo{$MethodVersion}{$Method}{"Abstract"})
        {
            if(my @SuperInterfaces = sort keys(%{$TypeInfo{$ClassVersion}{$ClassId}{"SuperInterface"}})) {
                push(@Search, @SuperInterfaces);
            }
        }
        
        foreach my $SuperId (@Search)
        {
            if($SuperId eq $ClassId) {
                next;
            }
            
            my $SuperName = getTypeName($SuperId, $ClassVersion);
            
            if(my $MethodInClass = findMethod_Class($Method, $SuperName, $ClassVersion)) {
                return $MethodInClass;
            }
            elsif(my $MethodInSuperClasses = findMethod($Method, $MethodVersion, $SuperName, $ClassVersion)) {
                return $MethodInSuperClasses;
            }
        }
    }
    
    my $TargetSuffix = getMSuffix($Method);
    my $TargetShortName = getMShort($Method);
    
    # search in java.lang.Object
    foreach my $C (keys(%JavaObjectMethod))
    {
        if($TargetSuffix eq getMSuffix($C))
        {
            if($TargetShortName eq getMShort($C)) {
                return $C;
            }
        }
    }
    
    return undef;
}

sub findMethod_Class($$$)
{
    my ($Method, $ClassName, $ClassVersion) = @_;
    
    my $TargetSuffix = getMSuffix($Method);
    my $TargetShortName = getMShort($Method);
    my $GenericName = getGeneric($ClassName);
    
    my @Candidates = ();
    
    if(defined $Class_Methods{$ClassVersion}{$ClassName}) {
        @Candidates = keys(%{$Class_Methods{$ClassVersion}{$ClassName}});
    }
    elsif(defined $Class_Methods_Generic{$ClassVersion}{$GenericName}) {
        @Candidates = keys(%{$Class_Methods_Generic{$ClassVersion}{$GenericName}});
    }
    else {
        return undef;
    }
    
    foreach my $Candidate (sort @Candidates)
    { # search for method with the same parameters suffix
        next if($MethodInfo{$ClassVersion}{$Candidate}{"Constructor"});
        
        if($TargetSuffix eq getMSuffix($Candidate))
        {
            if($TargetShortName eq getMShort($Candidate)) {
                return $Candidate;
            }
        }
    }
    
    return undef;
}

sub getBaseSignature($)
{
    my $Method = $_[0];
    $Method=~s/\)(.+)\Z/\)/g;
    return $Method;
}

sub prepareData($)
{
    my $LVer = $_[0];
    
    if(my $MUsed = $In::API{$LVer}{"MethodUsed"})
    {
        foreach my $M_Id (keys(%{$MUsed}))
        {
            my $Name = $MUsed->{$M_Id}{"Name"};
            $MethodUsed{$LVer}{$Name} = $MUsed->{$M_Id}{"Used"};
        }
    }
    
    foreach my $TypeId (keys(%{$TypeInfo{$LVer}}))
    {
        my $TypeAttr = $TypeInfo{$LVer}{$TypeId};
        my $TName = $TypeAttr->{"Name"};
        
        $TName_Tid{$LVer}{$TName} = $TypeId;
        
        if(defined $TypeAttr->{"Archive"})
        { # declaration
            $TName_Tid_Generic{$LVer}{getGeneric($TName)} = $TypeId;
        }
        
        if(not $TypeAttr->{"Dep"})
        {
            if(my $Archive = $TypeAttr->{"Archive"}) {
                $LibArchives{$LVer}{$Archive} = 1;
            }
        }
        
        foreach my $FieldName (keys(%{$TypeAttr->{"Fields"}}))
        {
            if($TypeAttr->{"Fields"}{$FieldName}{"Access"}=~/public|protected/) {
                $Class_Fields{$LVer}{$TName}{$FieldName} = $TypeAttr->{"Fields"}{$FieldName}{"Type"};
            }
        }
    }
    
    foreach my $Method (keys(%{$MethodInfo{$LVer}}))
    {
        my $Name = $MethodInfo{$LVer}{$Method}{"Name"};
        $MethodInfo{$LVer}{$Name} = delete($MethodInfo{$LVer}{$Method});
    }
    
    foreach my $Method (keys(%{$MethodInfo{$LVer}}))
    {
        my $MAttr = $MethodInfo{$LVer}{$Method};
        
        $MAttr->{"Signature"} = getSignature($Method, $LVer, "Full");
        
        if(my $ClassId = $MAttr->{"Class"}
        and $MAttr->{"Access"}=~/public|protected/)
        {
            my $CName = getTypeName($ClassId, $LVer);
            $Class_Methods{$LVer}{$CName}{$Method} = 1;
            $Class_Methods_Generic{$LVer}{getGeneric($CName)}{$Method} = 1;
            
            if($MAttr->{"Abstract"}) {
                $Class_AbstractMethods{$LVer}{$CName}{$Method} = 1;
            }
        }
        
        if($MAttr->{"Access"}!~/private/)
        {
            if($MAttr->{"Constructor"}) {
                registerUsage($MAttr->{"Class"}, $LVer);
            }
            else {
                registerUsage($MAttr->{"Return"}, $LVer);
            }
        }
        
        if($LVer==1 and not $MAttr->{"Constructor"}
        and my $BaseSign = getBaseSignature($Method)) {
            $OldMethodSignature{$BaseSign} = $Method;
        }
    }
}

sub mergeMethods()
{
    foreach my $Method (sort keys(%{$MethodInfo{1}}))
    { # compare methods
        next if(not defined $MethodInfo{2}{$Method});
        
        if(not $MethodInfo{1}{$Method}{"Archive"}
        or not $MethodInfo{2}{$Method}{"Archive"}) {
            next;
        }
        
        if(not methodFilter($Method, 1)) {
            next;
        }
        
        my $ClassId1 = $MethodInfo{1}{$Method}{"Class"};
        my $Class1_Name = getTypeName($ClassId1, 1);
        my $Class1_Type = getTypeType($ClassId1, 1);
        
        $CheckedTypes{$Class1_Name} = 1;
        $CheckedMethods{$Method} = 1;
        
        if(not $MethodInfo{1}{$Method}{"Static"}
        and $Class1_Type eq "class" and not $Class_Constructed{1}{$ClassId1})
        { # class cannot be constructed or inherited by clients
          # non-static method cannot be called
            next;
        }
        
        # checking attributes
        if(not $MethodInfo{1}{$Method}{"Static"}
        and $MethodInfo{2}{$Method}{"Static"}) {
            %{$CompatProblems{$Method}{"Method_Became_Static"}{""}} = ();
        }
        elsif($MethodInfo{1}{$Method}{"Static"}
        and not $MethodInfo{2}{$Method}{"Static"}) {
            %{$CompatProblems{$Method}{"Method_Became_NonStatic"}{""}} = ();
        }
        
        if(not $MethodInfo{1}{$Method}{"Synchronized"}
        and $MethodInfo{2}{$Method}{"Synchronized"}) {
            %{$CompatProblems{$Method}{"Method_Became_Synchronized"}{""}} = ();
        }
        elsif($MethodInfo{1}{$Method}{"Synchronized"}
        and not $MethodInfo{2}{$Method}{"Synchronized"}) {
            %{$CompatProblems{$Method}{"Method_Became_NonSynchronized"}{""}} = ();
        }
        
        if(not $MethodInfo{1}{$Method}{"Final"}
        and $MethodInfo{2}{$Method}{"Final"})
        {
            if($MethodInfo{1}{$Method}{"Static"}) {
                %{$CompatProblems{$Method}{"Static_Method_Became_Final"}{""}} = ();
            }
            else {
                %{$CompatProblems{$Method}{"NonStatic_Method_Became_Final"}{""}} = ();
            }
        }
        
        my $Access1 = $MethodInfo{1}{$Method}{"Access"};
        my $Access2 = $MethodInfo{2}{$Method}{"Access"};
        
        if($Access1 eq "public" and $Access2=~/protected|private/
        or $Access1 eq "protected" and $Access2=~/private/)
        {
            %{$CompatProblems{$Method}{"Changed_Method_Access"}{""}} = (
                "Old_Value"=>$Access1,
                "New_Value"=>$Access2);
        }
        
        my $Class2_Type = getTypeType($MethodInfo{2}{$Method}{"Class"}, 2);
        
        if($Class1_Type eq "class"
        and $Class2_Type eq "class")
        {
            if(not $MethodInfo{1}{$Method}{"Abstract"}
            and $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_Abstract"}{""}} = ();
                %{$CompatProblems{$Method}{"Class_Method_Became_Abstract"}{"this.".getSFormat($Method)}} = (
                    "Type_Name"=>$Class1_Name,
                    "Target"=>$Method);
            }
            elsif($MethodInfo{1}{$Method}{"Abstract"}
            and not $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_NonAbstract"}{""}} = ();
                %{$CompatProblems{$Method}{"Class_Method_Became_NonAbstract"}{"this.".getSFormat($Method)}} = (
                    "Type_Name"=>$Class1_Name,
                    "Target"=>$Method);
            }
        }
        elsif($Class1_Type eq "interface"
        and $Class2_Type eq "interface")
        {
            if(not $MethodInfo{1}{$Method}{"Abstract"}
            and $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_NonDefault"}{""}} = ();
                %{$CompatProblems{$Method}{"Interface_Method_Became_NonDefault"}{"this.".getSFormat($Method)}} = (
                    "Type_Name"=>$Class1_Name,
                    "Target"=>$Method);
            }
            elsif($MethodInfo{1}{$Method}{"Abstract"}
            and not $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_Default"}{""}} = ();
                %{$CompatProblems{$Method}{"Interface_Method_Became_Default"}{"this.".getSFormat($Method)}} = (
                    "Type_Name"=>$Class1_Name,
                    "Target"=>$Method);
            }
        }
        
        my %Exceptions_Old = map {getTypeName($_, 1) => $_} keys(%{$MethodInfo{1}{$Method}{"Exceptions"}});
        my %Exceptions_New = map {getTypeName($_, 2) => $_} keys(%{$MethodInfo{2}{$Method}{"Exceptions"}});
        foreach my $Exception (keys(%Exceptions_Old))
        {
            if(not $Exceptions_New{$Exception})
            {
                my $EType = getType($Exceptions_Old{$Exception}, 1);
                my $SuperClass = $EType->{"SuperClass"};
                
                if($KnownRuntimeExceptions{$Exception}
                or defined $SuperClass and getTypeName($SuperClass, 1) eq "java.lang.RuntimeException")
                {
                    if(not $MethodInfo{1}{$Method}{"Abstract"}
                    and not $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Removed_Unchecked_Exception"}{"this.".getSFormat($Exception)}} = (
                            "Type_Name"=>$Class1_Name,
                            "Target"=>$Exception);
                    }
                }
                else
                {
                    if($MethodInfo{1}{$Method}{"Abstract"}
                    and $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Abstract_Method_Removed_Checked_Exception"}{"this.".getSFormat($Exception)}} = (
                            "Type_Name"=>$Class1_Name,
                            "Target"=>$Exception);
                    }
                    else
                    {
                        %{$CompatProblems{$Method}{"NonAbstract_Method_Removed_Checked_Exception"}{"this.".getSFormat($Exception)}} = (
                            "Type_Name"=>$Class1_Name,
                            "Target"=>$Exception);
                    }
                }
            }
        }
        
        foreach my $Exception (keys(%Exceptions_New))
        {
            if(not $Exceptions_Old{$Exception})
            {
                my $EType = getType($Exceptions_New{$Exception}, 2);
                my $SuperClass = $EType->{"SuperClass"};
                
                if($KnownRuntimeExceptions{$Exception}
                or defined $SuperClass and getTypeName($SuperClass, 2) eq "java.lang.RuntimeException")
                {
                    if(not $MethodInfo{1}{$Method}{"Abstract"}
                    and not $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Added_Unchecked_Exception"}{"this.".getSFormat($Exception)}} = (
                            "Type_Name"=>$Class1_Name,
                            "Target"=>$Exception);
                    }
                }
                else
                {
                    if($MethodInfo{1}{$Method}{"Abstract"}
                    and $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Abstract_Method_Added_Checked_Exception"}{"this.".getSFormat($Exception)}} = (
                            "Type_Name"=>$Class1_Name,
                            "Target"=>$Exception);
                    }
                    else
                    {
                        %{$CompatProblems{$Method}{"NonAbstract_Method_Added_Checked_Exception"}{"this.".getSFormat($Exception)}} = (
                            "Type_Name"=>$Class1_Name,
                            "Target"=>$Exception);
                    }
                }
            }
        }
        
        if(defined $MethodInfo{1}{$Method}{"Param"})
        {
            foreach my $ParamPos (sort {int($a) <=> int($b)} keys(%{$MethodInfo{1}{$Method}{"Param"}}))
            { # checking parameters
                mergeParameters($Method, $ParamPos, $ParamPos);
            }
        }
        
        # check object type
        my $ObjectType1_Id = $MethodInfo{1}{$Method}{"Class"};
        my $ObjectType2_Id = $MethodInfo{2}{$Method}{"Class"};
        if($ObjectType1_Id and $ObjectType2_Id)
        {
            my $SubProblems = mergeTypes($ObjectType1_Id, $ObjectType2_Id);
            foreach my $SubProblemType (keys(%{$SubProblems}))
            {
                foreach my $SubLocation (keys(%{$SubProblems->{$SubProblemType}}))
                {
                    my $NewLocation = ($SubLocation)?"this.".$SubLocation:"this";
                    $CompatProblems{$Method}{$SubProblemType}{$NewLocation} = $SubProblems->{$SubProblemType}{$SubLocation};
                }
            }
        }
        # check return type
        my $ReturnType1_Id = $MethodInfo{1}{$Method}{"Return"};
        my $ReturnType2_Id = $MethodInfo{2}{$Method}{"Return"};
        if($ReturnType1_Id and $ReturnType2_Id)
        {
            my $SubProblems = mergeTypes($ReturnType1_Id, $ReturnType2_Id);
            foreach my $SubProblemType (keys(%{$SubProblems}))
            {
                foreach my $SubLocation (keys(%{$SubProblems->{$SubProblemType}}))
                {
                    my $NewLocation = ($SubLocation)?"retval.".$SubLocation:"retval";
                    $CompatProblems{$Method}{$SubProblemType}{$NewLocation} = $SubProblems->{$SubProblemType}{$SubLocation};
                }
            }
        }
    }
}

sub mergeParameters($$$)
{
    my ($Method, $ParamPos1, $ParamPos2) = @_;
    if(not $Method or not defined $MethodInfo{1}{$Method}{"Param"}
    or not defined $MethodInfo{2}{$Method}{"Param"}) {
        return;
    }
    
    my $ParamType1_Id = $MethodInfo{1}{$Method}{"Param"}{$ParamPos1}{"Type"};
    my $ParamType2_Id = $MethodInfo{2}{$Method}{"Param"}{$ParamPos2}{"Type"};
    
    if(not $ParamType1_Id or not $ParamType2_Id) {
        return;
    }
    
    my $Parameter_Name = $MethodInfo{1}{$Method}{"Param"}{$ParamPos1}{"Name"};
    my $Parameter_Location = ($Parameter_Name)?$Parameter_Name:showPos($ParamPos1)." Parameter";
    
    # checking type declaration changes
    my $SubProblems = mergeTypes($ParamType1_Id, $ParamType2_Id);
    
    my $Type1 = getType($ParamType1_Id, 1);
    my $Type2 = getType($ParamType2_Id, 2);
    
    if($Type1->{"Name"} ne $Type2->{"Name"})
    {
        if(index($Type1->{"Name"}, "...")!=-1 and index($Type2->{"Name"}, "[]")!=-1)
        {
            %{$CompatProblems{$Method}{"Variable_Arity_To_Array"}{$Parameter_Name}} = (
                "Type_Name"=>getTypeName($MethodInfo{1}{$Method}{"Class"}, 1),
                "Param_Name"=>$Parameter_Name,
                "Old_Value"=>$Type1->{"Name"},
                "New_Value"=>$Type2->{"Name"});
        }
        elsif(index($Type1->{"Name"}, "[]")!=-1 and index($Type2->{"Name"}, "...")!=-1)
        {
            %{$CompatProblems{$Method}{"Array_To_Variable_Arity"}{$Parameter_Name}} = (
                "Type_Name"=>getTypeName($MethodInfo{1}{$Method}{"Class"}, 1),
                "Param_Name"=>$Parameter_Name,
                "Old_Value"=>$Type1->{"Name"},
                "New_Value"=>$Type2->{"Name"});
        }
    }
    
    foreach my $SubProblemType (keys(%{$SubProblems}))
    {
        foreach my $SubLocation (keys(%{$SubProblems->{$SubProblemType}}))
        {
            my $NewLocation = ($SubLocation)?$Parameter_Location.".".$SubLocation:$Parameter_Location;
            $CompatProblems{$Method}{$SubProblemType}{$NewLocation} = $SubProblems->{$SubProblemType}{$SubLocation};
        }
    }
}

sub detectTypeChange($$$$$)
{
    my ($Ct1, $Ct2, $Type1_Id, $Type2_Id, $Prefix) = @_;
    my %LocalProblems = ();
    
    my $Type1 = getType($Type1_Id, 1);
    my $Type2 = getType($Type2_Id, 2);
    
    my $Type1_Name = $Type1->{"Name"};
    my $Type2_Name = $Type2->{"Name"};
    
    my $Type1_Show = $Type1_Name;
    my $Type2_Show = $Type2_Name;
    
    if(defined $Ct1->{"GenericParam"}
    and defined $Ct1->{"GenericParam"}{$Type1_Name})
    {
        $Type1_Name = getTypeName($Ct1->{"GenericParam"}{$Type1_Name}, 1);
        $Type1_Show .= " extends ".$Type1_Name;
    }
    
    if(defined $Ct2->{"GenericParam"}
    and defined $Ct2->{"GenericParam"}{$Type2_Name})
    {
        $Type2_Name = getTypeName($Ct2->{"GenericParam"}{$Type2_Name}, 2);
        $Type2_Show .= " extends ".$Type2_Name;
    }
    
    my $Type1_Base = undef;
    my $Type2_Base = undef;
    
    if($Type1->{"Type"} eq "array") {
        $Type1_Base = getOneStepBaseType($Type1_Id, 1);
    }
    else {
        $Type1_Base = getBaseType($Type1_Id, 1);
    }
    
    if($Type2->{"Type"} eq "array") {
        $Type2_Base = getOneStepBaseType($Type2_Id, 2);
    }
    else {
        $Type2_Base = getBaseType($Type2_Id, 2);
    }
    
    return () if(not $Type1_Name or not $Type2_Name);
    return () if(not $Type1_Base->{"Name"} or not $Type2_Base->{"Name"});
    
    if($Type1_Base->{"Name"} ne $Type2_Base->{"Name"} and $Type1_Name eq $Type2_Name)
    {# base type change
        %{$LocalProblems{"Changed_".$Prefix."_BaseType"}}=(
            "Old_Value"=>$Type1_Base->{"Name"},
            "New_Value"=>$Type2_Base->{"Name"});
    }
    elsif($Type1_Name ne $Type2_Name)
    {# type change
        %{$LocalProblems{"Changed_".$Prefix."_Type"}}=(
            "Old_Value"=>$Type1_Show,
            "New_Value"=>$Type2_Show);
    }
    return %LocalProblems;
}

sub specChars($)
{
    my $Str = $_[0];
    if(not defined $Str
    or $Str eq "") {
        return "";
    }
    $Str=~s/\&([^#]|\Z)/&amp;$1/g;
    $Str=~s/</&lt;/g;
    $Str=~s/\-\>/&#45;&gt;/g; # &minus;
    $Str=~s/>/&gt;/g;
    $Str=~s/([^ ])( )([^ ])/$1\@ALONE_SP\@$3/g;
    $Str=~s/ /&#160;/g; # &nbsp;
    $Str=~s/\@ALONE_SP\@/ /g;
    $Str=~s/\n/<br\/>/g;
    $Str=~s/\"/&quot;/g;
    $Str=~s/\'/&#39;/g;
    return $Str;
}

sub blackName($)
{
    my $N = $_[0];
    return "<span class='iname_b'>".$N."</span>";
}

sub highLight_ItalicColor($$)
{
    my ($M, $V) = @_;
    return getSignature($M, $V, "Full|HTML|Italic|Color");
}

sub getSignature($$$)
{
    my ($Method, $LVer, $Kind) = @_;
    if(defined $Cache{"getSignature"}{$LVer}{$Method}{$Kind}) {
        return $Cache{"getSignature"}{$LVer}{$Method}{$Kind};
    }
    
    # settings
    my ($Full, $Html, $Simple, $Italic, $Color,
    $ShowParams, $ShowClass, $ShowAttr, $Desc,
    $ShowReturn, $Target) = (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, undef);
    
    if($Kind=~/Full/) {
        $Full = 1;
    }
    if($Kind=~/HTML/) {
        $Html = 1;
    }
    if($Kind=~/Simple/) {
        $Simple = 1;
    }
    if($Kind=~/Italic/) {
        $Italic = 1;
    }
    if($Kind=~/Color/) {
        $Color = 1;
    }
    if($Kind=~/Target=(\d+)/) {
        $Target = $1;
    }
    if($Kind=~/Param/) {
        $ShowParams = 1;
    }
    if($Kind=~/Class/) {
        $ShowClass = 1;
    }
    if($Kind=~/Attr/) {
        $ShowAttr = 1;
    }
    if($Kind=~/Desc/) {
        $Desc = 1;
    }
    if($Kind=~/Return/) {
        $ShowReturn = 1;
    }
    
    if(not defined $MethodInfo{$LVer}{$Method}{"ShortName"})
    { # from java.lang.Object
        if($Html) {
            return specChars($Method);
        }
        else {
            return $Method;
        }
    }
    
    my $Signature = "";
    
    my $ShortName = $MethodInfo{$LVer}{$Method}{"ShortName"};
    
    if($Html) {
        $ShortName = specChars($ShortName);
    }
    
    $Signature .= $ShortName;
    
    if($Full or $ShowClass)
    {
        my $Class = getTypeName($MethodInfo{$LVer}{$Method}{"Class"}, $LVer);
        
        if($In::Opt{"HideTemplates"}) {
            $Class=~s/<.*>//g;
        }
        
        if($Html) {
            $Class = specChars($Class);
        }
        
        $Signature = $Class.".".$Signature;
    }
    my @Params = ();
    
    if(defined $MethodInfo{$LVer}{$Method}{"Param"})
    {
        foreach my $PPos (sort {int($a)<=>int($b)}
        keys(%{$MethodInfo{$LVer}{$Method}{"Param"}}))
        {
            my $PTid = $MethodInfo{$LVer}{$Method}{"Param"}{$PPos}{"Type"};
            if(my $PTName = getTypeName($PTid, $LVer))
            {
                if($In::Opt{"HideTemplates"}) {
                    $PTName=~s/<.*>//g;
                }
                
                if(not $In::Opt{"ShowPackages"}) {
                    $PTName=~s/(\A|\<\s*|\,\s*)[a-z0-9\.]+\./$1/g;
                }
                
                if($Html) {
                    $PTName = specChars($PTName);
                }
                
                if($Full or $ShowParams)
                {
                    my $PName = $MethodInfo{$LVer}{$Method}{"Param"}{$PPos}{"Name"};
                    
                    if($Simple) {
                        $PName = "<i>$PName</i>";
                    }
                    elsif($Html)
                    {
                        my $Style = "param";
                        
                        if(defined $Target
                        and $Target==$PPos) {
                            $PName = "<span class='focus_p'>$PName</span>";
                        }
                        elsif($Color) {
                            $PName = "<span class='color_p'>$PName</span>";
                        }
                        else {
                            $PName = "<i>$PName</i>";
                        }
                    }
                    
                    push(@Params, $PTName." ".$PName);
                }
                else {
                    push(@Params, $PTName);
                }
            }
        }
    }
    
    if($Simple) {
        $Signature = "<b>".$Signature."</b>";
    }
    
    if($Html and not $Simple)
    {
        $Signature .= "&#160;";
        if($Desc) {
            $Signature .= "<span class='sym_pd'>";
        }
        else {
            $Signature .= "<span class='sym_p'>";
        }
        if(@Params)
        {
            foreach my $Pos (0 .. $#Params)
            {
                my $Name = "";
                
                if($Pos==0) {
                    $Name .= "(&#160;";
                }
                
                $Name .= $Params[$Pos];
                
                $Name = "<span>".$Name."</span>";
                
                if($Pos==$#Params) {
                    $Name .= "&#160;)";
                }
                else {
                    $Name .= ", ";
                }
                
                $Signature .= $Name;
            }
        }
        else {
            $Signature .= "(&#160;)";
        }
        $Signature .= "</span>";
    }
    else
    {
        if(@Params) {
            $Signature .= " ( ".join(", ", @Params)." )";
        }
        else {
            $Signature .= " ( )";
        }
    }
    
    if($Full or $ShowAttr)
    {
        if($MethodInfo{$LVer}{$Method}{"Static"}) {
            $Signature .= " [static]";
        }
        elsif($MethodInfo{$LVer}{$Method}{"Abstract"}) {
            $Signature .= " [abstract]";
        }
    }
    
    if($Full)
    {
        if($In::Opt{"ShowAccess"})
        {
            if(my $Access = $MethodInfo{$LVer}{$Method}{"Access"})
            {
                if($Access ne "public") {
                    $Signature .= " [".$Access."]";
                }
            }
        }
    }
    
    if($Full or $ShowReturn)
    {
        if(my $ReturnId = $MethodInfo{$LVer}{$Method}{"Return"})
        {
            my $RName = getTypeName($ReturnId, $LVer);
            
            if($In::Opt{"HideTemplates"}) {
                $RName=~s/<.*>//g;
            }
            
            if(not $In::Opt{"ShowPackages"}) {
                $RName=~s/(\A|\<\s*|\,\s*)[a-z0-9\.]+\./$1/g;
            }
            
            if($Desc) {
                $Signature = "<b>".specChars($RName)."</b> ".$Signature;
            }
            elsif($Simple) {
                $Signature .= " <b>:</b> ".specChars($RName);
            }
            elsif($Html) {
                $Signature .= "<span class='sym_p nowrap'> &#160;<b>:</b>&#160;&#160;".specChars($RName)."</span>";
            }
            else {
                $Signature .= " : ".$RName;
            }
        }
    }
    
    if($Full)
    {
        if(not $In::Opt{"SkipDeprecated"})
        {
            if($MethodInfo{$LVer}{$Method}{"Deprecated"}) {
                $Signature .= " *DEPRECATED*";
            }
        }
    }
    
    $Signature=~s/java\.lang\.//g;
    
    if($Html)
    {
        if(not $In::Opt{"SkipDeprecated"}) {
            $Signature=~s!(\*deprecated\*)!<span class='deprecated'>$1</span>!ig;
        }
        
        $Signature=~s!(\[static\]|\[abstract\]|\[public\]|\[private\]|\[protected\])!<span class='attr'>$1</span>!g;
    }
    
    if($Simple) {
        $Signature=~s/\[\]/\[ \]/g;
    }
    elsif($Html)
    {
        $Signature=~s!\[\]![&#160;]!g;
        $Signature=~s!operator=!operator&#160;=!g;
    }
    
    return ($Cache{"getSignature"}{$LVer}{$Method}{$Kind} = $Signature);
}

sub getReportHeader($)
{
    my $Level = $_[0];
    my $Report_Header = "<h1>";
    if($Level eq "Source") {
        $Report_Header .= "Source compatibility";
    }
    elsif($Level eq "Binary") {
        $Report_Header .= "Binary compatibility";
    }
    else {
        $Report_Header .= "API compatibility";
    }
    $Report_Header .= " report for the <span style='color:Blue;'>".$In::Opt{"TargetTitle"}."</span> library between <span style='color:Red;'>".$In::Desc{1}{"Version"}."</span> and <span style='color:Red;'>".$In::Desc{2}{"Version"}."</span> versions";
    if($In::Opt{"ClientPath"}) {
        $Report_Header .= " (concerning portability of the client: <span style='color:Blue;'>".getFilename($In::Opt{"ClientPath"})."</span>)";
    }
    $Report_Header .= "</h1>\n";
    return $Report_Header;
}

sub getSourceInfo()
{
    my $CheckedArchives = "<a name='Checked_Archives'></a>";
    if($In::Opt{"OldStyle"}) {
        $CheckedArchives .= "<h2>Java Archives (".keys(%{$LibArchives{1}}).")</h2>";
    }
    else {
        $CheckedArchives .= "<h2>Java Archives <span class='gray'>&nbsp;".keys(%{$LibArchives{1}})."&nbsp;</span></h2>";
    }
    $CheckedArchives .= "\n<hr/><div class='jar_list'>\n";
    foreach my $ArchivePath (sort {lc($a) cmp lc($b)}  keys(%{$LibArchives{1}})) {
        $CheckedArchives .= getFilename($ArchivePath)."<br/>\n";
    }
    $CheckedArchives .= "</div><br/>$TOP_REF<br/>\n";
    return $CheckedArchives;
}

sub getTypeProblemsCount($$)
{
    my ($TargetSeverity, $Level) = @_;
    my $Type_Problems_Count = 0;
    
    foreach my $Type_Name (sort keys(%{$TypeChanges{$Level}}))
    {
        my %Kinds_Target = ();
        foreach my $Kind (sort keys(%{$TypeChanges{$Level}{$Type_Name}}))
        {
            if($CompatRules{$Level}{$Kind}{"Severity"} ne $TargetSeverity) {
                next;
            }
            
            foreach my $Location (sort keys(%{$TypeChanges{$Level}{$Type_Name}{$Kind}}))
            {
                my $Target = $TypeChanges{$Level}{$Type_Name}{$Kind}{$Location}{"Target"};
                
                if($Kinds_Target{$Kind}{$Target}) {
                    next;
                }
                
                $Kinds_Target{$Kind}{$Target} = 1;
                $Type_Problems_Count += 1;
            }
        }
    }
    
    return $Type_Problems_Count;
}

sub showNum($)
{
    if($_[0])
    {
        my $Num = cutNum($_[0], 2, 0);
        if($Num eq "0")
        {
            foreach my $P (3 .. 7)
            {
                $Num = cutNum($_[0], $P, 1);
                if($Num ne "0") {
                    last;
                }
            }
        }
        if($Num eq "0") {
            $Num = $_[0];
        }
        return $Num;
    }
    return $_[0];
}

sub cutNum($$$)
{
    my ($num, $digs_to_cut, $z) = @_;
    if($num!~/\./)
    {
        $num .= ".";
        foreach (1 .. $digs_to_cut-1) {
            $num .= "0";
        }
    }
    elsif($num=~/\.(.+)\Z/ and length($1)<$digs_to_cut-1)
    {
        foreach (1 .. $digs_to_cut - 1 - length($1)) {
            $num .= "0";
        }
    }
    elsif($num=~/\d+\.(\d){$digs_to_cut,}/) {
      $num=sprintf("%.".($digs_to_cut-1)."f", $num);
    }
    $num=~s/\.[0]+\Z//g;
    if($z) {
        $num=~s/(\.[1-9]+)[0]+\Z/$1/g;
    }
    return $num;
}

sub getSummary($)
{
    my $Level = $_[0];
    my ($Added, $Removed, $M_Problems_High, $M_Problems_Medium, $M_Problems_Low,
    $T_Problems_High, $T_Problems_Medium, $T_Problems_Low, $M_Other, $T_Other) = (0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    
    %{$RESULT{$Level}} = (
        "Problems"=>0,
        "Warnings"=>0,
        "Affected"=>0);
    
    # check rules
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (keys(%{$CompatProblems{$Method}}))
        {
            if(not defined $CompatRules{"Binary"}{$Kind} and not defined $CompatRules{"Source"}{$Kind})
            { # unknown rule
                if(not $UnknownRules{$Level}{$Kind})
                { # only one warning
                    printMsg("WARNING", "unknown rule \"$Kind\" (\"$Level\")");
                    $UnknownRules{$Level}{$Kind}=1;
                }
            }
        }
    }
    
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($CompatRules{$Level}{$Kind}{"Kind"} eq "Methods")
            {
                my $Severity = $CompatRules{$Level}{$Kind}{"Severity"};
                foreach my $Loc (sort keys(%{$CompatProblems{$Method}{$Kind}}))
                {
                    if($Kind eq "Added_Method")
                    {
                        if($Level eq "Source")
                        {
                            if($ChangedReturnFromVoid{$Method}) {
                                next;
                            }
                        }
                        $Added+=1;
                    }
                    elsif($Kind eq "Removed_Method")
                    {
                        if($Level eq "Source")
                        {
                            if($ChangedReturnFromVoid{$Method}) {
                                next;
                            }
                        }
                        $Removed+=1;
                        $TotalAffected{$Level}{$Method} = $Severity;
                    }
                    else
                    {
                        if($Severity eq "Safe") {
                            $M_Other += 1;
                        }
                        elsif($Severity eq "High") {
                            $M_Problems_High+=1;
                        }
                        elsif($Severity eq "Medium") {
                            $M_Problems_Medium+=1;
                        }
                        elsif($Severity eq "Low") {
                            $M_Problems_Low+=1;
                        }
                        if(($Severity ne "Low" or $In::Opt{"StrictCompat"})
                        and $Severity ne "Safe") {
                            $TotalAffected{$Level}{$Method} = $Severity;
                        }
                    }
                }
            }
        }
    }
    
    my %MethodTypeIndex = ();
    
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($CompatRules{$Level}{$Kind}{"Kind"} eq "Types")
            {
                my $Severity = $CompatRules{$Level}{$Kind}{"Severity"};
                if(($Severity ne "Low" or $In::Opt{"StrictCompat"})
                and $Severity ne "Safe")
                {
                    if(my $Sev = $TotalAffected{$Level}{$Method})
                    {
                        if($Severity_Val{$Severity}>$Severity_Val{$Sev}) {
                            $TotalAffected{$Level}{$Method} = $Severity;
                        }
                    }
                    else {
                        $TotalAffected{$Level}{$Method} = $Severity;
                    }
                }
                
                my $MK = $CompatProblems{$Method}{$Kind};
                my (@Locs1, @Locs2) = ();
                foreach my $Loc (sort {length($a)<=>length($b)} sort keys(%{$MK}))
                {
                    if(index($Loc, "retval")==0 or index($Loc, "this")==0) {
                        push(@Locs2, $Loc);
                    }
                    else {
                        push(@Locs1, $Loc);
                    }
                }
                foreach my $Loc (@Locs1, @Locs2)
                {
                    my $Type = $MK->{$Loc}{"Type_Name"};
                    my $Target = $MK->{$Loc}{"Target"};
                    
                    if(defined $MethodTypeIndex{$Method}{$Type}{$Kind}{$Target})
                    { # one location for one type and target
                        next;
                    }
                    $MethodTypeIndex{$Method}{$Type}{$Kind}{$Target} = 1;
                    
                    $TypeChanges{$Level}{$Type}{$Kind}{$Loc} = $MK->{$Loc};
                    $TypeProblemsIndex{$Level}{$Type}{$Kind}{$Loc}{$Method} = 1;
                }
            }
        }
    }
    
    %MethodTypeIndex = (); # clear memory
    
    $T_Problems_High = getTypeProblemsCount("High", $Level);
    $T_Problems_Medium = getTypeProblemsCount("Medium", $Level);
    $T_Problems_Low = getTypeProblemsCount("Low", $Level);
    $T_Other = getTypeProblemsCount("Safe", $Level);
    
    my $SCount = keys(%CheckedMethods)-$Added;
    if($SCount)
    {
        my %Weight = (
            "High" => 100,
            "Medium" => 50,
            "Low" => 25
        );
        foreach (keys(%{$TotalAffected{$Level}})) {
            $RESULT{$Level}{"Affected"}+=$Weight{$TotalAffected{$Level}{$_}};
        }
        $RESULT{$Level}{"Affected"} = $RESULT{$Level}{"Affected"}/$SCount;
    }
    else {
        $RESULT{$Level}{"Affected"} = 0;
    }
    $RESULT{$Level}{"Affected"} = showNum($RESULT{$Level}{"Affected"});
    if($RESULT{$Level}{"Affected"}>=100) {
        $RESULT{$Level}{"Affected"} = 100;
    }
    
    my ($TestInfo, $TestResults, $Problem_Summary) = ();
    
    # test info
    $TestInfo .= "<h2>Test Info</h2><hr/>\n";
    $TestInfo .= "<table class='summary'>\n";
    $TestInfo .= "<tr><th>Library Name</th><td>".$In::Opt{"TargetTitle"}."</td></tr>\n";
    $TestInfo .= "<tr><th>Version #1</th><td>".$In::Desc{1}{"Version"}."</td></tr>\n";
    $TestInfo .= "<tr><th>Version #2</th><td>".$In::Desc{2}{"Version"}."</td></tr>\n";
    
    if($In::Opt{"JoinReport"})
    {
        if($Level eq "Binary") {
            $TestInfo .= "<tr><th>Subject</th><td width='150px'>Binary Compatibility</td></tr>\n"; # Run-time
        }
        if($Level eq "Source") {
            $TestInfo .= "<tr><th>Subject</th><td width='150px'>Source Compatibility</td></tr>\n"; # Build-time
        }
    }
    $TestInfo .= "</table>\n";
    
    # test results
    $TestResults .= "<h2>Test Results</h2><hr/>\n";
    $TestResults .= "<table class='summary'>\n";
    
    my $Checked_Archives_Link = "0";
    $Checked_Archives_Link = "<a href='#Checked_Archives' style='color:Blue;'>".keys(%{$LibArchives{1}})."</a>" if(keys(%{$LibArchives{1}})>0);
    
    $TestResults .= "<tr><th>Total JARs</th><td>$Checked_Archives_Link</td></tr>\n";
    $TestResults .= "<tr><th>Total Methods / Classes</th><td>".keys(%CheckedMethods)." / ".keys(%CheckedTypes)."</td></tr>\n";
    
    $RESULT{$Level}{"Problems"} += $Removed+$M_Problems_High+$T_Problems_High+$T_Problems_Medium+$M_Problems_Medium;
    if($In::Opt{"StrictCompat"}) {
        $RESULT{$Level}{"Problems"}+=$T_Problems_Low+$M_Problems_Low;
    }
    else {
        $RESULT{$Level}{"Warnings"}+=$T_Problems_Low+$M_Problems_Low;
    }
    
    my $META_DATA = "kind:".lc($Level).";";
    $META_DATA .= $RESULT{$Level}{"Problems"}?"verdict:incompatible;":"verdict:compatible;";
    $TestResults .= "<tr><th>Compatibility</th>\n";
    
    my $BC_Rate = showNum(100 - $RESULT{$Level}{"Affected"});
    
    if($RESULT{$Level}{"Problems"})
    {
        my $Cl = "incompatible";
        if($BC_Rate>=90) {
            $Cl = "warning";
        }
        elsif($BC_Rate>=80) {
            $Cl = "almost_compatible";
        }
        
        $TestResults .= "<td class=\'$Cl\'>".$BC_Rate."%</td>\n";
    }
    else
    {
        $TestResults .= "<td class=\'compatible\'>100%</td>\n";
    }
    
    $TestResults .= "</tr>\n";
    $TestResults .= "</table>\n";
    
    $META_DATA .= "affected:".$RESULT{$Level}{"Affected"}.";";# in percents
    
    # Problem Summary
    $Problem_Summary .= "<h2>Problem Summary</h2><hr/>\n";
    $Problem_Summary .= "<table class='summary'>\n";
    $Problem_Summary .= "<tr><th></th><th style='text-align:center;'>Severity</th><th style='text-align:center;'>Count</th></tr>\n";
    
    my $Added_Link = "0";
    if($Added>0)
    {
        if($In::Opt{"ShortMode"}) {
            $Added_Link = $Added;
        }
        else
        {
            if($In::Opt{"JoinReport"}) {
                $Added_Link = "<a href='#".$Level."_Added' style='color:Blue;'>$Added</a>";
            }
            else {
                $Added_Link = "<a href='#Added' style='color:Blue;'>$Added</a>";
            }
        }
    }
    $META_DATA .= "added:$Added;";
    $Problem_Summary .= "<tr><th>Added Methods</th><td>-</td><td".getStyle("M", "Added", $Added).">$Added_Link</td></tr>\n";
    
    my $Removed_Link = "0";
    if($Removed>0)
    {
        if($In::Opt{"ShortMode"}) {
            $Removed_Link = $Removed;
        }
        else
        {
            if($In::Opt{"JoinReport"}) {
                $Removed_Link = "<a href='#".$Level."_Removed' style='color:Blue;'>$Removed</a>"
            }
            else {
                $Removed_Link = "<a href='#Removed' style='color:Blue;'>$Removed</a>"
            }
        }
    }
    $META_DATA .= "removed:$Removed;";
    $Problem_Summary .= "<tr><th>Removed Methods</th>";
    $Problem_Summary .= "<td>High</td><td".getStyle("M", "Removed", $Removed).">$Removed_Link</td></tr>\n";
    
    my $TH_Link = "0";
    $TH_Link = "<a href='#".getAnchor("Type", $Level, "High")."' style='color:Blue;'>$T_Problems_High</a>" if($T_Problems_High>0);
    $META_DATA .= "type_problems_high:$T_Problems_High;";
    $Problem_Summary .= "<tr><th rowspan='3'>Problems with<br/>Data Types</th>";
    $Problem_Summary .= "<td>High</td><td".getStyle("T", "High", $T_Problems_High).">$TH_Link</td></tr>\n";
    
    my $TM_Link = "0";
    $TM_Link = "<a href='#".getAnchor("Type", $Level, "Medium")."' style='color:Blue;'>$T_Problems_Medium</a>" if($T_Problems_Medium>0);
    $META_DATA .= "type_problems_medium:$T_Problems_Medium;";
    $Problem_Summary .= "<tr><td>Medium</td><td".getStyle("T", "Medium", $T_Problems_Medium).">$TM_Link</td></tr>\n";
    
    my $TL_Link = "0";
    $TL_Link = "<a href='#".getAnchor("Type", $Level, "Low")."' style='color:Blue;'>$T_Problems_Low</a>" if($T_Problems_Low>0);
    $META_DATA .= "type_problems_low:$T_Problems_Low;";
    $Problem_Summary .= "<tr><td>Low</td><td".getStyle("T", "Low", $T_Problems_Low).">$TL_Link</td></tr>\n";
    
    my $MH_Link = "0";
    $MH_Link = "<a href='#".getAnchor("Method", $Level, "High")."' style='color:Blue;'>$M_Problems_High</a>" if($M_Problems_High>0);
    $META_DATA .= "method_problems_high:$M_Problems_High;";
    $Problem_Summary .= "<tr><th rowspan='3'>Problems with<br/>Methods</th>";
    $Problem_Summary .= "<td>High</td><td".getStyle("M", "High", $M_Problems_High).">$MH_Link</td></tr>\n";
    
    my $MM_Link = "0";
    $MM_Link = "<a href='#".getAnchor("Method", $Level, "Medium")."' style='color:Blue;'>$M_Problems_Medium</a>" if($M_Problems_Medium>0);
    $META_DATA .= "method_problems_medium:$M_Problems_Medium;";
    $Problem_Summary .= "<tr><td>Medium</td><td".getStyle("M", "Medium", $M_Problems_Medium).">$MM_Link</td></tr>\n";
    
    my $ML_Link = "0";
    $ML_Link = "<a href='#".getAnchor("Method", $Level, "Low")."' style='color:Blue;'>$M_Problems_Low</a>" if($M_Problems_Low>0);
    $META_DATA .= "method_problems_low:$M_Problems_Low;";
    $Problem_Summary .= "<tr><td>Low</td><td".getStyle("M", "Low", $M_Problems_Low).">$ML_Link</td></tr>\n";
    
    # Safe Changes
    if($T_Other)
    {
        my $TS_Link = "<a href='#".getAnchor("Type", $Level, "Safe")."' style='color:Blue;'>$T_Other</a>";
        $Problem_Summary .= "<tr><th>Other Changes<br/>in Data Types</th><td>-</td><td".getStyle("T", "Safe", $T_Other).">$TS_Link</td></tr>\n";
    }
    
    if($M_Other)
    {
        my $MS_Link = "<a href='#".getAnchor("Method", $Level, "Safe")."' style='color:Blue;'>$M_Other</a>";
        $Problem_Summary .= "<tr><th>Other Changes<br/>in Methods</th><td>-</td><td".getStyle("M", "Safe", $M_Other).">$MS_Link</td></tr>\n";
    }
    $META_DATA .= "checked_methods:".keys(%CheckedMethods).";";
    $META_DATA .= "checked_types:".keys(%CheckedTypes).";";
    $META_DATA .= "tool_version:$TOOL_VERSION";
    $Problem_Summary .= "</table>\n";
    
    my $AnyChanged = ($Added or $Removed or $M_Problems_High or $M_Problems_Medium or $M_Problems_Low or
    $T_Problems_High or $T_Problems_Medium or $T_Problems_Low or $M_Other or $T_Other);
    
    return ($TestInfo.$TestResults.$Problem_Summary, $META_DATA, $AnyChanged);
}

sub getStyle($$$)
{
    my ($Subj, $Act, $Num) = @_;
    my %Style = (
        "Added"=>"new",
        "Removed"=>"failed",
        "Safe"=>"passed",
        "Low"=>"warning",
        "Medium"=>"failed",
        "High"=>"failed"
    );
    
    if($Num>0) {
        return " class='".$Style{$Act}."'";
    }
    
    return "";
}

sub getAnchor($$$)
{
    my ($Kind, $Level, $Severity) = @_;
    if($In::Opt{"JoinReport"})
    {
        if($Severity eq "Safe") {
            return "Other_".$Level."_Changes_In_".$Kind."s";
        }
        else {
            return $Kind."_".$Level."_Problems_".$Severity;
        }
    }
    else
    {
        if($Severity eq "Safe") {
            return "Other_Changes_In_".$Kind."s";
        }
        else {
            return $Kind."_Problems_".$Severity;
        }
    }
}

sub getReportAdded($)
{
    if($In::Opt{"ShortMode"}) {
        return "";
    }
    
    my $Level = $_[0];
    my ($ADDED_METHODS, %MethodAddedInArchiveClass);
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($Kind eq "Added_Method")
            {
                my $ArchiveName = $MethodInfo{2}{$Method}{"Archive"};
                my $ClassName = getShortName($MethodInfo{2}{$Method}{"Class"}, 2);
                if($Level eq "Source")
                {
                    if($ChangedReturnFromVoid{$Method}) {
                        next;
                    }
                }
                $MethodAddedInArchiveClass{$ArchiveName}{$ClassName}{$Method} = 1;
            }
        }
    }
    my $Added_Number = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%MethodAddedInArchiveClass))
    {
        foreach my $ClassName (sort {lc($a) cmp lc($b)} keys(%{$MethodAddedInArchiveClass{$ArchiveName}}))
        {
            my %NameSpace_Method = ();
            foreach my $Method (keys(%{$MethodAddedInArchiveClass{$ArchiveName}{$ClassName}})) {
                $NameSpace_Method{$MethodInfo{2}{$Method}{"Package"}}{$Method} = 1;
            }
            
            my $ShowClass = $ClassName;
            $ShowClass=~s/<.*>//g;
            
            foreach my $NameSpace (sort keys(%NameSpace_Method))
            {
                $ADDED_METHODS .= "<span class='jar'>$ArchiveName</span>, <span class='cname'>".specChars($ShowClass).".class</span><br/>\n";
                
                if($NameSpace) {
                    $ADDED_METHODS .= "<span class='pkg_t'>package</span> <span class='pkg'>$NameSpace</span><br/>\n";
                }
                
                if($In::Opt{"Compact"}) {
                    $ADDED_METHODS .= "<div class='symbols'>";
                }
                
                my @SortedMethods = sort {lc($MethodInfo{2}{$a}{"Signature"}) cmp lc($MethodInfo{2}{$b}{"Signature"})} sort keys(%{$NameSpace_Method{$NameSpace}});
                foreach my $Method (@SortedMethods)
                {
                    $Added_Number += 1;
                    
                    my $Signature = undef;
                    
                    if($In::Opt{"Compact"}) {
                        $Signature = getSignature($Method, 2, "Full|HTML|Simple");
                    }
                    else {
                        $Signature = highLight_ItalicColor($Method, 2);
                    }
                    
                    if($NameSpace) {
                        $Signature=~s/(\W|\A)\Q$NameSpace\E\.(\w)/$1$2/g;
                    }
                    
                    if($In::Opt{"Compact"}) {
                        $ADDED_METHODS .= "&nbsp;".$Signature."<br/>\n";
                    }
                    else {
                        $ADDED_METHODS .= insertIDs($ContentSpanStart.$Signature.$ContentSpanEnd."<br/>\n".$ContentDivStart."<span class='mngl'>".specChars($Method)."</span><br/><br/>".$ContentDivEnd."\n");
                    }
                }
                
                if($In::Opt{"Compact"}) {
                    $ADDED_METHODS .= "</div>";
                }
                
                $ADDED_METHODS .= "<br/>\n";
            }
            
        }
    }
    if($ADDED_METHODS)
    {
        my $Anchor = "<a name='Added'></a>";
        if($In::Opt{"JoinReport"}) {
            $Anchor = "<a name='".$Level."_Added'></a>";
        }
        if($In::Opt{"OldStyle"}) {
            $ADDED_METHODS = "<h2>Added Methods ($Added_Number)</h2><hr/>\n".$ADDED_METHODS;
        }
        else {
            $ADDED_METHODS = "<h2>Added Methods <span".getStyle("M", "Added", $Added_Number).">&nbsp;$Added_Number&nbsp;</span></h2><hr/>\n".$ADDED_METHODS;
        }
        $ADDED_METHODS = $Anchor.$ADDED_METHODS.$TOP_REF."<br/>\n";
    }
    return $ADDED_METHODS;
}

sub getReportRemoved($)
{
    if($In::Opt{"ShortMode"}) {
        return "";
    }
    
    my $Level = $_[0];
    my ($REMOVED_METHODS, %MethodRemovedFromArchiveClass);
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($Kind eq "Removed_Method")
            {
                if($Level eq "Source")
                {
                    if($ChangedReturnFromVoid{$Method}) {
                        next;
                    }
                }
                my $ArchiveName = $MethodInfo{1}{$Method}{"Archive"};
                my $ClassName = getShortName($MethodInfo{1}{$Method}{"Class"}, 1);
                $MethodRemovedFromArchiveClass{$ArchiveName}{$ClassName}{$Method} = 1;
            }
        }
    }
    my $Removed_Number = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%MethodRemovedFromArchiveClass))
    {
        foreach my $ClassName (sort {lc($a) cmp lc($b)} keys(%{$MethodRemovedFromArchiveClass{$ArchiveName}}))
        {
            my %NameSpace_Method = ();
            foreach my $Method (keys(%{$MethodRemovedFromArchiveClass{$ArchiveName}{$ClassName}}))
            {
                $NameSpace_Method{$MethodInfo{1}{$Method}{"Package"}}{$Method} = 1;
            }
            
            my $ShowClass = $ClassName;
            $ShowClass=~s/<.*>//g;
            
            foreach my $NameSpace (sort keys(%NameSpace_Method))
            {
                $REMOVED_METHODS .= "<span class='jar'>$ArchiveName</span>, <span class='cname'>".specChars($ShowClass).".class</span><br/>\n";
                
                if($NameSpace) {
                    $REMOVED_METHODS .= "<span class='pkg_t'>package</span> <span class='pkg'>$NameSpace</span><br/>\n";
                }
                
                if($In::Opt{"Compact"}) {
                    $REMOVED_METHODS .= "<div class='symbols'>";
                }
                
                my @SortedMethods = sort {lc($MethodInfo{1}{$a}{"Signature"}) cmp lc($MethodInfo{1}{$b}{"Signature"})} sort keys(%{$NameSpace_Method{$NameSpace}});
                foreach my $Method (@SortedMethods)
                {
                    $Removed_Number += 1;
                    
                    my $Signature = undef;
                    
                    if($In::Opt{"Compact"}) {
                        $Signature = getSignature($Method, 1, "Full|HTML|Simple");
                    }
                    else {
                        $Signature = highLight_ItalicColor($Method, 1);
                    }
                    
                    if($NameSpace) {
                        $Signature=~s/(\W|\A)\Q$NameSpace\E\.(\w)/$1$2/g;
                    }
                    
                    if($In::Opt{"Compact"}) {
                        $REMOVED_METHODS .= "&nbsp;".$Signature."<br/>\n";
                    }
                    else {
                        $REMOVED_METHODS .= insertIDs($ContentSpanStart.$Signature.$ContentSpanEnd."<br/>\n".$ContentDivStart."<span class='mngl'>".specChars($Method)."</span><br/><br/>".$ContentDivEnd."\n");
                    }
                }
                
                if($In::Opt{"Compact"}) {
                    $REMOVED_METHODS .= "</div>";
                }
                
                $REMOVED_METHODS .= "<br/>\n";
            }
        }
    }
    if($REMOVED_METHODS)
    {
        my $Anchor = "<a name='Removed'></a><a name='Withdrawn'></a>";
        if($In::Opt{"JoinReport"}) {
            $Anchor = "<a name='".$Level."_Removed'></a><a name='".$Level."_Withdrawn'></a>";
        }
        if($In::Opt{"OldStyle"}) {
            $REMOVED_METHODS = "<h2>Removed Methods ($Removed_Number)</h2><hr/>\n".$REMOVED_METHODS;
        }
        else {
            $REMOVED_METHODS = "<h2>Removed Methods <span".getStyle("M", "Removed", $Removed_Number).">&nbsp;$Removed_Number&nbsp;</span></h2><hr/>\n".$REMOVED_METHODS;
        }
        $REMOVED_METHODS = $Anchor.$REMOVED_METHODS.$TOP_REF."<br/>\n";
    }
    return $REMOVED_METHODS;
}

sub readRules($)
{
    my $Kind = $_[0];
    if(not -f $RULES_PATH{$Kind}) {
        exitStatus("Module_Error", "can't access \'".$RULES_PATH{$Kind}."\'");
    }
    my $Content = readFile($RULES_PATH{$Kind});
    while(my $Rule = parseTag(\$Content, "rule"))
    {
        my $RId = parseTag(\$Rule, "id");
        my @Properties = ("Severity", "Change", "Effect", "Overcome", "Kind");
        foreach my $Prop (@Properties) {
            if(my $Value = parseTag(\$Rule, lc($Prop)))
            {
                $Value=~s/\n[ ]*//;
                $CompatRules{$Kind}{$RId}{$Prop} = $Value;
            }
        }
        if($CompatRules{$Kind}{$RId}{"Kind"}=~/\A(Methods|Parameters)\Z/) {
            $CompatRules{$Kind}{$RId}{"Kind"} = "Methods";
        }
        else { # Types, Fields
            $CompatRules{$Kind}{$RId}{"Kind"} = "Types";
        }
    }
}

sub addMarkup($)
{
    my $Content = $_[0];
    
    # auto-markup
    $Content=~s/\n[ ]*//; # spaces
    $Content=~s!([2-9]\))!<br/>$1!g; # 1), 2), ...
    if($Content=~/\ANOTE:/)
    { # notes
        $Content=~s!(NOTE):!<b>$1</b>:!g;
    }
    else {
        $Content=~s!(NOTE):!<br/><br/><b>$1</b>:!g;
    }
    
    my @Keywords = (
        "static",
        "abstract",
        "default",
        "final",
        "synchronized"
    );
    
    my $MKeys = join("|", @Keywords);
    foreach (@Keywords) {
        $MKeys .= "|non-".$_;
    }
    
    $Content=~s!(became\s*)($MKeys)([^\w-]|\Z)!$1<b>$2</b>$3!ig; # intrinsic types, modifiers
    
    # Markdown
    $Content=~s!\*\*([\w\-\@]+)\*\*!<b>$1</b>!ig;
    $Content=~s!\*([\w\-]+)\*!<i>$1</i>!ig;
    
    return $Content;
}

sub applyMacroses($$$$$$)
{
    my ($Level, $Subj, $Kind, $Content, $Problem, $AddAttr) = @_;
    
    $Content = addMarkup($Content);
    
    # macros
    foreach my $Attr (sort {$b cmp $a} (keys(%{$Problem}), keys(%{$AddAttr})))
    {
        my $Macro = "\@".lc($Attr);
        my $Value = undef;
        
        if(defined $Problem->{$Attr}) {
            $Value = $Problem->{$Attr};
        }
        else {
            $Value = $AddAttr->{$Attr};
        }
        
        if(not defined $Value
        or $Value eq "") {
            next;
        }
        
        if(index($Content, $Macro)==-1) {
            next;
        }
        
        if($Attr eq "Param_Pos") {
            $Value = showPos($Value);
        }
        
        if($Attr eq "Invoked") {
            $Value = blackName(specChars($Value));
        }
        elsif($Value=~/\s/) {
            $Value = "<span class='value'>".specChars($Value)."</span>";
        }
        else
        {
            my $Fmt = "Class|HTML|Desc";
            
            if($Attr ne "Invoked_By")
            {
                if($Attr eq "Method_Short"
                or $Kind!~/Overridden|Moved_Up/) {
                    $Fmt = "HTML|Desc";
                }
            }
            
            if($Subj eq "Change") {
                $Fmt .= "|Return";
            }
            
            if(defined $MethodInfo{1}{$Value}
            and defined $MethodInfo{1}{$Value}{"ShortName"}) {
                $Value = blackName(getSignature($Value, 1, $Fmt));
            }
            elsif(defined $MethodInfo{2}{$Value}
            and defined $MethodInfo{2}{$Value}{"ShortName"}) {
                $Value = blackName(getSignature($Value, 2, $Fmt));
            }
            else
            {
                $Value = specChars($Value);
                if($Attr ne "Type_Type") {
                    $Value = "<b>".$Value."</b>";
                }
            }
        }
        $Content=~s/\Q$Macro\E/$Value/g;
    }
    
    if($Content=~/(\A|[^\@\w])(\@\w+)/)
    {
        if(not $IncompleteRules{$Level}{$Kind})
        { # only one warning
            printMsg("WARNING", "incomplete $2 in the rule \"$Kind\" (\"$Level\")");
            $IncompleteRules{$Level}{$Kind} = 1;
        }
    }
    
    return $Content;
}

sub getReportMethodProblems($$)
{
    my ($TargetSeverity, $Level) = @_;
    my $METHOD_PROBLEMS = "";
    my (%ReportMap, %MethodChanges) = ();
    
    foreach my $Method (sort keys(%CompatProblems))
    {
        my $ArchiveName = $MethodInfo{1}{$Method}{"Archive"};
        my $ClassName = getShortName($MethodInfo{1}{$Method}{"Class"}, 1);
        
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($CompatRules{$Level}{$Kind}{"Kind"} eq "Methods")
            {
                if($Kind eq "Added_Method"
                or $Kind eq "Removed_Method") {
                    next;
                }
                
                if(my $Severity = $CompatRules{$Level}{$Kind}{"Severity"})
                {
                    if($Severity ne $TargetSeverity) {
                        next;
                    }
                    
                    $MethodChanges{$Method}{$Kind} = $CompatProblems{$Method}{$Kind};
                    $ReportMap{$ArchiveName}{$ClassName}{$Method} = 1;
                }
            }
        }
    }
    my $ProblemsNum = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%ReportMap))
    {
        foreach my $ClassName (sort {lc($a) cmp lc($b)} keys(%{$ReportMap{$ArchiveName}}))
        {
            my %NameSpace_Method = ();
            foreach my $Method (keys(%{$ReportMap{$ArchiveName}{$ClassName}})) {
                $NameSpace_Method{$MethodInfo{1}{$Method}{"Package"}}{$Method} = 1;
            }
            
            my $ShowClass = $ClassName;
            $ShowClass=~s/<.*>//g;
            
            foreach my $NameSpace (sort keys(%NameSpace_Method))
            {
                $METHOD_PROBLEMS .= "<span class='jar'>$ArchiveName</span>, <span class='cname'>".specChars($ShowClass).".class</span><br/>\n";
                if($NameSpace) {
                    $METHOD_PROBLEMS .= "<span class='pkg_t'>package</span> <span class='pkg'>$NameSpace</span><br/>\n";
                }
                
                my @SortedMethods = sort {lc($MethodInfo{1}{$a}{"Signature"}) cmp lc($MethodInfo{1}{$b}{"Signature"})} sort keys(%{$NameSpace_Method{$NameSpace}});
                foreach my $Method (@SortedMethods)
                {
                    my %AddAttr = ();
                    
                    $AddAttr{"Method_Short"} = $Method;
                    $AddAttr{"Class"} = getTypeName($MethodInfo{1}{$Method}{"Class"}, 1);
                    
                    my $METHOD_REPORT = "";
                    my $ProblemNum = 1;
                    foreach my $Kind (sort keys(%{$MethodChanges{$Method}}))
                    {
                        foreach my $Loc (sort keys(%{$MethodChanges{$Method}{$Kind}}))
                        {
                            my $ProblemAttr = $MethodChanges{$Method}{$Kind}{$Loc};
                            
                            if(my $Change = applyMacroses($Level, "Change", $Kind, $CompatRules{$Level}{$Kind}{"Change"}, $ProblemAttr, \%AddAttr))
                            {
                                my $Effect = applyMacroses($Level, "Effect", $Kind, $CompatRules{$Level}{$Kind}{"Effect"}, $ProblemAttr, \%AddAttr);
                                $METHOD_REPORT .= "<tr>\n<th>$ProblemNum</th>\n<td>".$Change."</td>\n<td>".$Effect."</td>\n</tr>\n";
                                $ProblemNum += 1;
                                $ProblemsNum += 1;
                            }
                        }
                    }
                    $ProblemNum -= 1;
                    if($METHOD_REPORT)
                    {
                        my $ShowMethod = highLight_ItalicColor($Method, 1);
                        if($NameSpace)
                        {
                            $METHOD_REPORT = cutNs($METHOD_REPORT, $NameSpace);
                            $ShowMethod = cutNs($ShowMethod, $NameSpace);
                        }
                        
                        $METHOD_PROBLEMS .= $ContentSpanStart."<span class='ext'>[+]</span> ".$ShowMethod;
                        if($In::Opt{"OldStyle"}) {
                            $METHOD_PROBLEMS .= " ($ProblemNum)";
                        }
                        else {
                            $METHOD_PROBLEMS .= " <span".getStyle("M", $TargetSeverity, $ProblemNum).">&nbsp;$ProblemNum&nbsp;</span>";
                        }
                        $METHOD_PROBLEMS .= $ContentSpanEnd."<br/>\n";
                        $METHOD_PROBLEMS .= $ContentDivStart;
                        
                        if(not $In::Opt{"Compact"}) {
                            $METHOD_PROBLEMS .= "<span class='mngl pleft'>".specChars($Method)."</span><br/>\n";
                        }
                        
                        $METHOD_PROBLEMS .= "<table class='ptable'><tr><th width='2%'></th><th width='47%'>Change</th><th>Effect</th></tr>$METHOD_REPORT</table><br/>$ContentDivEnd\n";
                        
                    }
                }
                
                $METHOD_PROBLEMS .= "<br/>";
            }
        }
    }
    if($METHOD_PROBLEMS)
    {
        $METHOD_PROBLEMS = insertIDs($METHOD_PROBLEMS);
        
        my $Title = "Problems with Methods, $TargetSeverity Severity";
        if($TargetSeverity eq "Safe")
        { # Safe Changes
            $Title = "Other Changes in Methods";
        }
        if($In::Opt{"OldStyle"}) {
            $METHOD_PROBLEMS = "<h2>$Title ($ProblemsNum)</h2><hr/>\n".$METHOD_PROBLEMS;
        }
        else {
            $METHOD_PROBLEMS = "<h2>$Title <span".getStyle("M", $TargetSeverity, $ProblemsNum).">&nbsp;$ProblemsNum&nbsp;</span></h2><hr/>\n".$METHOD_PROBLEMS;
        }
        $METHOD_PROBLEMS = "<a name='".getAnchor("Method", $Level, $TargetSeverity)."'></a>\n".$METHOD_PROBLEMS;
        $METHOD_PROBLEMS .= $TOP_REF."<br/>\n";
    }
    return $METHOD_PROBLEMS;
}

sub showType($$$)
{
    my ($Name, $Html, $LVer) = @_;
    my $TType = $TypeInfo{$LVer}{$TName_Tid{$LVer}{$Name}}{"Type"};
    if($Html) {
        $Name = "<span class='ttype'>".$TType."</span> ".specChars($Name);
    }
    else {
        $Name = $TType." ".$Name;
    }
    return $Name;
}

sub getReportTypeProblems($$)
{
    my ($TargetSeverity, $Level) = @_;
    my $TYPE_PROBLEMS = "";
    
    my %ReportMap = ();
    my %TypeChanges_Sev = ();
    
    foreach my $TypeName (keys(%{$TypeChanges{$Level}}))
    {
        my $ArchiveName = $TypeInfo{1}{$TName_Tid{1}{$TypeName}}{"Archive"};
        
        foreach my $Kind (keys(%{$TypeChanges{$Level}{$TypeName}}))
        {
            if($CompatRules{$Level}{$Kind}{"Severity"} ne $TargetSeverity) {
                next;
            }
            
            foreach my $Loc (keys(%{$TypeChanges{$Level}{$TypeName}{$Kind}}))
            {
                $ReportMap{$ArchiveName}{$TypeName} = 1;
                $TypeChanges_Sev{$TypeName}{$Kind}{$Loc} = $TypeChanges{$Level}{$TypeName}{$Kind}{$Loc};
            }
        }
    }
    
    my $ProblemsNum = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%ReportMap))
    {
        my %NameSpace_Type = ();
        foreach my $TypeName (keys(%{$ReportMap{$ArchiveName}})) {
            $NameSpace_Type{$TypeInfo{1}{$TName_Tid{1}{$TypeName}}{"Package"}}{$TypeName} = 1;
        }
        foreach my $NameSpace (sort keys(%NameSpace_Type))
        {
            $TYPE_PROBLEMS .= "<span class='jar'>$ArchiveName</span><br/>\n";
            if($NameSpace) {
                $TYPE_PROBLEMS .= "<span class='pkg_t'>package</span> <span class='pkg'>".$NameSpace."</span><br/>\n";
            }
            
            my @SortedTypes = sort {lc(showType($a, 0, 1)) cmp lc(showType($b, 0, 1))} keys(%{$NameSpace_Type{$NameSpace}});
            foreach my $TypeName (@SortedTypes)
            {
                my $TypeId = $TName_Tid{1}{$TypeName};
                
                my $ProblemNum = 1;
                my $TYPE_REPORT = "";
                my (%Kinds_Locations, %Kinds_Target) = ();
                
                foreach my $Kind (sort keys(%{$TypeChanges_Sev{$TypeName}}))
                {
                    foreach my $Location (sort keys(%{$TypeChanges_Sev{$TypeName}{$Kind}}))
                    {
                        $Kinds_Locations{$Kind}{$Location} = 1;
                        
                        my $Target = $TypeChanges_Sev{$TypeName}{$Kind}{$Location}{"Target"};
                        if($Kinds_Target{$Kind}{$Target}) {
                            next;
                        }
                        $Kinds_Target{$Kind}{$Target} = 1;
                        
                        my %AddAttr = ();
                        
                        if($Kind=~/Method/)
                        {
                            if(defined $MethodInfo{1}{$Target} and $MethodInfo{1}{$Target}{"Name"})
                            {
                                $AddAttr{"Method_Short"} = $Target;
                                $AddAttr{"Class"} = getTypeName($MethodInfo{1}{$Target}{"Class"}, 1);
                            }
                            elsif(defined $MethodInfo{2}{$Target} and $MethodInfo{2}{$Target}{"Name"})
                            {
                                $AddAttr{"Method_Short"} = $Target;
                                $AddAttr{"Class"} = getTypeName($MethodInfo{2}{$Target}{"Class"}, 2);
                            }
                        }
                        
                        my $ProblemAttr = $TypeChanges_Sev{$TypeName}{$Kind}{$Location};
                        
                        if(my $Change = applyMacroses($Level, "Change", $Kind, $CompatRules{$Level}{$Kind}{"Change"}, $ProblemAttr, \%AddAttr))
                        {
                            my $Effect = applyMacroses($Level, "Effect", $Kind, $CompatRules{$Level}{$Kind}{"Effect"}, $ProblemAttr, \%AddAttr);
                            
                            $TYPE_REPORT .= "<tr>\n<th>$ProblemNum</th>\n<td>".$Change."</td>\n<td>".$Effect."</td>\n</tr>\n";
                            $ProblemNum += 1;
                            $ProblemsNum += 1;
                        }
                    }
                }
                $ProblemNum -= 1;
                if($TYPE_REPORT)
                {
                    my $Affected = "";
                    if(not defined $TypeInfo{1}{$TypeId}{"Annotation"}) {
                        $Affected = getAffectedMethods($Level, $TypeName, \%Kinds_Locations);
                    }
                    
                    my $ShowType = showType($TypeName, 1, 1);
                    if($NameSpace)
                    {
                        $TYPE_REPORT = cutNs($TYPE_REPORT, $NameSpace);
                        $ShowType = cutNs($ShowType, $NameSpace);
                        $Affected = cutNs($Affected, $NameSpace);
                    }
                    
                    $TYPE_PROBLEMS .= $ContentSpanStart."<span class='ext'>[+]</span> ".$ShowType;
                    if($In::Opt{"OldStyle"}) {
                        $TYPE_PROBLEMS .= " ($ProblemNum)";
                    }
                    else {
                        $TYPE_PROBLEMS .= " <span".getStyle("T", $TargetSeverity, $ProblemNum).">&nbsp;$ProblemNum&nbsp;</span>";
                    }
                    $TYPE_PROBLEMS .= $ContentSpanEnd."<br/>\n";
                    $TYPE_PROBLEMS .= $ContentDivStart."<table class='ptable'><tr>";
                    $TYPE_PROBLEMS .= "<th width='2%'></th><th width='47%'>Change</th><th>Effect</th>";
                    $TYPE_PROBLEMS .= "</tr>$TYPE_REPORT</table>".$Affected."<br/><br/>$ContentDivEnd\n";
                }
            }
            
            $TYPE_PROBLEMS .= "<br/>";
        }
    }
    if($TYPE_PROBLEMS)
    {
        $TYPE_PROBLEMS = insertIDs($TYPE_PROBLEMS);
        
        my $Title = "Problems with Data Types, $TargetSeverity Severity";
        if($TargetSeverity eq "Safe")
        { # Safe Changes
            $Title = "Other Changes in Data Types";
        }
        if($In::Opt{"OldStyle"}) {
            $TYPE_PROBLEMS = "<h2>$Title ($ProblemsNum)</h2><hr/>\n".$TYPE_PROBLEMS;
        }
        else {
            $TYPE_PROBLEMS = "<h2>$Title <span".getStyle("T", $TargetSeverity, $ProblemsNum).">&nbsp;$ProblemsNum&nbsp;</span></h2><hr/>\n".$TYPE_PROBLEMS;
        }
        $TYPE_PROBLEMS = "<a name='".getAnchor("Type", $Level, $TargetSeverity)."'></a>\n".$TYPE_PROBLEMS;
        $TYPE_PROBLEMS .= $TOP_REF."<br/>\n";
    }
    return $TYPE_PROBLEMS;
}

sub cutNs($$)
{
    my ($N, $Ns) = @_;
    $N=~s/(\W|\A)\Q$Ns\E\.(\w)/$1$2/g;
    return $N;
}

sub getAffectedMethods($$$)
{
    my ($Level, $Target_TypeName, $Kinds_Locations) = @_;
    
    my $LIMIT = 10;
    if(defined $In::Opt{"AffectLimit"}) {
        $LIMIT = $In::Opt{"AffectLimit"};
    }
    
    my %SymSel = ();
    
    foreach my $Kind (sort keys(%{$Kinds_Locations}))
    {
        my @Locs = sort {(index($a, "retval")!=-1) cmp (index($b, "retval")!=-1)} sort {length($a)<=>length($b)} sort keys(%{$Kinds_Locations->{$Kind}});
        
        foreach my $Loc (@Locs)
        {
            foreach my $Method (keys(%{$TypeProblemsIndex{$Level}{$Target_TypeName}{$Kind}{$Loc}}))
            {
                if($Method eq ".client_method") {
                    next;
                }
                
                if(not defined $SymSel{$Method})
                {
                    $SymSel{$Method}{"Kind"} = $Kind;
                    $SymSel{$Method}{"Loc"} = $Loc;
                }
            }
        }
    }
    
    my $Total = keys(%SymSel);
    
    if(not $Total) {
        return "";
    }
    
    my $Affected = "";
    my $SNum = 0;
    
    foreach my $Method (sort {lc($a) cmp lc($b)} keys(%SymSel))
    {
        my $Kind = $SymSel{$Method}{"Kind"};
        my $Loc = $SymSel{$Method}{"Loc"};
        
        my $Desc = getAffectDesc($Method, $Kind, $Loc, $Level);
        my $PName = getParamName($Loc);
        my $Pos = getParamPos($PName, $Method, 1);
        
        $Affected .= "<span class='iname_a'>".getSignature($Method, 1, "HTML|Italic|Param|Class|Target=".$Pos)."</span><br/>";
        $Affected .= "<div class='affect'>".$Desc."</div>\n";
        
        if(++$SNum>=$LIMIT) {
            last;
        }
    }
    
    if($Total>$LIMIT) {
        $Affected .= " <b>...</b>\n<br/>\n"; # and others ...
    }
    
    $Affected = "<div class='affected'>".$Affected."</div>";
    if($Affected)
    {
        my $Per = showNum($Total*100/keys(%CheckedMethods));
        $Affected =  $ContentDivStart.$Affected.$ContentDivEnd;
        $Affected =  $ContentSpanStart_Affected."[+] affected methods: $Total ($Per\%)".$ContentSpanEnd.$Affected;
    }
    
    return $Affected;
}

sub getAffectDesc($$$$)
{
    my ($Method, $Kind, $Location, $Level) = @_;
    my %Affect = %{$CompatProblems{$Method}{$Kind}{$Location}};
    my $New_Value = $Affect{"New_Value"};
    my $Type_Name = $Affect{"Type_Name"};
    my @Sentence_Parts = ();
    
    $Location=~s/\.[^.]+?\Z//;
    
    my $TypeAttr = getType($MethodInfo{1}{$Method}{"Class"}, 1);
    my $Type_Type = $TypeAttr->{"Type"};
    
    my $ABSTRACT_M = $MethodInfo{1}{$Method}{"Abstract"}?" abstract":"";
    my $ABSTRACT_C = $TypeAttr->{"Abstract"}?" abstract":"";
    my $METHOD_TYPE = $MethodInfo{1}{$Method}{"Constructor"}?"constructor":"method";
    
    if($Kind eq "Class_Overridden_Method" or $Kind eq "Class_Method_Moved_Up_Hierarchy") {
        return "Method '".getSignature($New_Value, 2, "Class|HTML|Italic")."' will be called instead of this method in a client program.";
    }
    elsif($CompatRules{$Level}{$Kind}{"Kind"} eq "Types")
    {
        my %MInfo = %{$MethodInfo{1}{$Method}};
        
        if($Location eq "this") {
            return "This$ABSTRACT_M $METHOD_TYPE is from \'".specChars($Type_Name)."\'$ABSTRACT_C $Type_Type.";
        }
        
        my $TypeID = undef;
        
        if($Location=~/retval/)
        { # return value
            if($Location=~/\./) {
                push(@Sentence_Parts, "Field \'".specChars($Location)."\' in the return value");
            }
            else {
                push(@Sentence_Parts, "Return value");
            }
            
            $TypeID = $MInfo{"Return"};
        }
        elsif($Location=~/this/)
        { # "this" reference
            push(@Sentence_Parts, "Field \'".specChars($Location)."\' in the object");
            
            $TypeID = $MInfo{"Class"};
        }
        else
        { # parameters
            my $PName = getParamName($Location);
            my $PPos = getParamPos($PName, $Method, 1);
            
            if($Location=~/\./) {
                push(@Sentence_Parts, "Field \'".specChars($Location)."\' in ".showPos($PPos)." parameter");
            }
            else {
                push(@Sentence_Parts, showPos($PPos)." parameter");
            }
            if($PName) {
                push(@Sentence_Parts, "\'$PName\'");
            }
            
            if(defined $MInfo{"Param"}) {
                $TypeID = $MInfo{"Param"}{$PPos}{"Type"};
            }
        }
        push(@Sentence_Parts, " of this$ABSTRACT_M method");
        
        my $Location_T = $Location;
        $Location_T=~s/\A\w+(\.|\Z)//; # location in type
        
        my $TypeID_Problem = $TypeID;
        if($Location_T) {
            $TypeID_Problem = getFieldType($Location_T, $TypeID, 1);
        }
        
        if($TypeInfo{1}{$TypeID_Problem}{"Name"} eq $Type_Name) {
            push(@Sentence_Parts, "is of type \'".specChars($Type_Name)."\'.");
        }
        else {
            push(@Sentence_Parts, "has base type \'".specChars($Type_Name)."\'.");
        }
    }
    return join(" ", @Sentence_Parts);
}

sub getParamPos($$$)
{
    my ($Name, $Method, $LVer) = @_;
    
    if(defined $MethodInfo{$LVer}{$Method}
    and defined $MethodInfo{$LVer}{$Method}{"Param"})
    {
        my $Info = $MethodInfo{$LVer}{$Method};
        foreach (keys(%{$Info->{"Param"}}))
        {
            if($Info->{"Param"}{$_}{"Name"} eq $Name)
            {
                return $_;
            }
        }
    }
    
    return undef;
}

sub getParamName($)
{
    my $Loc = $_[0];
    $Loc=~s/\..*//g;
    return $Loc;
}

sub getFieldType($$$)
{
    my ($Location, $TypeId, $LVer) = @_;
    
    my @Fields = split(/\./, $Location);
    
    foreach my $Name (@Fields)
    {
        my $TInfo = getBaseType($TypeId, $LVer);
        
        foreach my $N (keys(%{$TInfo->{"Fields"}}))
        {
            if($N eq $Name)
            {
                $TypeId = $TInfo->{"Fields"}{$N}{"Type"};
                last;
            }
        }
    }
    
    return $TypeId;
}

sub writeReport($$)
{
    my ($Level, $Report) = @_;
    my $RPath = getReportPath($Level);
    writeFile($RPath, $Report);
}

sub createReport()
{
    if($In::Opt{"JoinReport"}) {
        writeReport("Join", getReport("Join"));
    }
    elsif($In::Opt{"DoubleReport"})
    { # default
        writeReport("Binary", getReport("Binary"));
        writeReport("Source", getReport("Source"));
    }
    elsif($In::Opt{"BinaryOnly"})
    { # --binary
        writeReport("Binary", getReport("Binary"));
    }
    elsif($In::Opt{"SourceOnly"})
    { # --source
        writeReport("Source", getReport("Source"));
    }
}

sub getCssStyles($)
{
    my $Level = $_[0];
    
    my $CssStyles = readModule("Css", "Report.css");
    
    if($Level eq "Join" or $In::Opt{"ExternCss"}) {
        $CssStyles .= readModule("Css", "Tabs.css");
    }
    
    return $CssStyles;
}

sub getJsScript($)
{
    my $Level = $_[0];
    
    my $JScripts = readModule("Js", "Sections.js");
    
    if($Level eq "Join" or $In::Opt{"ExternJs"}) {
        $JScripts .= readModule("Js", "Tabs.js");
    }
    
    return $JScripts;
}

sub getReport($)
{
    my $Level = $_[0];
    
    my $CssStyles = getCssStyles($Level);
    my $JScripts = getJsScript($Level);
    
    if(defined $In::Opt{"ExternCss"}) {
        writeFile($In::Opt{"ExternCss"}, $CssStyles);
    }
    
    if(defined $In::Opt{"ExternJs"}) {
        writeFile($In::Opt{"ExternJs"}, $JScripts);
    }
    
    if($Level eq "Join")
    {
        my $Title = $In::Opt{"TargetTitle"}.": ".$In::Desc{1}{"Version"}." to ".$In::Desc{2}{"Version"}." compatibility report";
        my $Keywords = $In::Opt{"TargetTitle"}.", compatibility";
        my $Description = "Compatibility report for the ".$In::Opt{"TargetTitle"}." library between ".$In::Desc{1}{"Version"}." and ".$In::Desc{2}{"Version"}." versions";
        
        my ($BSummary, $BMetaData, $BAnyChanged) = getSummary("Binary");
        my ($SSummary, $SMetaData, $SAnyChanged) = getSummary("Source");
        
        my $Report = "<!-\- $BMetaData -\->\n<!-\- $SMetaData -\->\n".composeHTML_Head($Level, $Title, $Keywords, $Description, $CssStyles, $JScripts, ($BAnyChanged or $SAnyChanged))."<body><a name='Source'></a><a name='Binary'></a><a name='Top'></a>";
        
        $Report .= getReportHeader("Join");
        $Report .= "<br/><div class='tabset'>\n";
        $Report .= "<a id='BinaryID' href='#BinaryTab' class='tab active'>Binary<br/>Compatibility</a>\n";
        $Report .= "<a id='SourceID' href='#SourceTab' style='margin-left:3px' class='tab disabled'>Source<br/>Compatibility</a>\n";
        $Report .= "</div>\n";
        
        $Report .= "<div id='BinaryTab' class='tab'>\n$BSummary\n".getReportAdded("Binary").getReportRemoved("Binary").getReportProblems("High", "Binary").getReportProblems("Medium", "Binary").getReportProblems("Low", "Binary").getReportProblems("Safe", "Binary").getSourceInfo()."<br/><br/><br/></div>";
        
        $Report .= "<div id='SourceTab' class='tab'>\n$SSummary\n".getReportAdded("Source").getReportRemoved("Source").getReportProblems("High", "Source").getReportProblems("Medium", "Source").getReportProblems("Low", "Source").getReportProblems("Safe", "Source").getSourceInfo()."<br/><br/><br/></div>";
        
        $Report .= getReportFooter();
        $Report .= "\n</body></html>";
        return $Report;
    }
    else
    {
        my ($Summary, $MetaData, $AnyChanged) = getSummary($Level);
        
        my $Title = $In::Opt{"TargetTitle"}.": ".$In::Desc{1}{"Version"}." to ".$In::Desc{2}{"Version"}." ".lc($Level)." compatibility report";
        my $Keywords = $In::Opt{"TargetTitle"}.", ".lc($Level).", compatibility";
        my $Description = "$Level compatibility report for the ".$In::Opt{"TargetTitle"}." library between ".$In::Desc{1}{"Version"}." and ".$In::Desc{2}{"Version"}." versions";
        
        my $Report = "<!-\- $MetaData -\->\n".composeHTML_Head($Level, $Title, $Keywords, $Description, $CssStyles, $JScripts, $AnyChanged)."<body><a name='Top'></a>";
        $Report .= getReportHeader($Level)."\n".$Summary."\n";
        $Report .= getReportAdded($Level).getReportRemoved($Level);
        $Report .= getReportProblems("High", $Level).getReportProblems("Medium", $Level).getReportProblems("Low", $Level).getReportProblems("Safe", $Level);
        $Report .= getSourceInfo()."<br/><br/><br/>\n";
        $Report .= getReportFooter();
        $Report .= "\n</body></html>";
        return $Report;
    }
}

sub getReportFooter()
{
    my $Footer = "";
    $Footer .= "<hr/>";
    $Footer .= "<div class='footer' align='right'><i>Generated by ";
    $Footer .= "<a href='".$HomePage{"Dev"}."'>Java API Compliance Checker</a> $TOOL_VERSION &#160;";
    $Footer .= "</i></div>";
    $Footer .= "<br/>";
    return $Footer;
}

sub getReportProblems($$)
{
    my ($Priority, $Level) = @_;
    my $Report = getReportTypeProblems($Priority, $Level);
    if(my $MProblems = getReportMethodProblems($Priority, $Level)) {
        $Report .= $MProblems;
    }
    if($Report)
    {
        if($In::Opt{"JoinReport"})
        {
            if($Priority eq "Safe") {
                $Report = "<a name=\'Other_".$Level."_Changes\'></a>".$Report;
            }
            else {
                $Report = "<a name=\'".$Priority."_Risk_".$Level."_Problems\'></a>".$Report;
            }
        }
        else
        {
            if($Priority eq "Safe") {
                $Report = "<a name=\'Other_Changes\'></a>".$Report;
            }
            else {
                $Report = "<a name=\'".$Priority."_Risk_Problems\'></a>".$Report;
            }
        }
    }
    return $Report;
}

sub composeHTML_Head($$$$$$$)
{
    my ($Level, $Title, $Keywords, $Description, $Styles, $Scripts, $AnyChanged) = @_;
    
    my $Head = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
    $Head .= "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n";
    $Head .= "<head>\n";
    $Head .= "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n";
    $Head .= "<meta name=\"keywords\" content=\"$Keywords\" />\n";
    $Head .= "<meta name=\"description\" content=\"$Description\" />\n";
    
    if(not $AnyChanged) {
        $Head .= "<meta name=\"robots\" content=\"noindex\" />\n";
    }
    
    my $RPath = getReportPath($Level);
    
    if(defined $In::Opt{"ExternCss"}) {
        $Head .= "<link rel=\"stylesheet\" type=\"text/css\" href=\"".getRelPath($In::Opt{"ExternCss"}, $RPath)."\" />\n";
    }
    
    if(defined $In::Opt{"ExternJs"}) {
        $Head .= "<script type=\"text/javascript\" src=\"".getRelPath($In::Opt{"ExternJs"}, $RPath)."\"></script>\n";
    }
    
    $Head .= "<title>$Title</title>\n";
    
    if(not defined $In::Opt{"ExternCss"}) {
        $Head .= "<style type=\"text/css\">\n$Styles\n</style>\n";
    }
    
    if(not defined $In::Opt{"ExternJs"}) {
        $Head .= "<script type=\"text/javascript\" language=\"JavaScript\">\n<!--\n$Scripts\n-->\n</script>\n";
    }
    
    $Head .= "</head>\n";
    
    return $Head;
}

sub insertIDs($)
{
    my $Text = $_[0];
    while($Text=~/CONTENT_ID/)
    {
        if(int($Content_Counter)%2)
        {
            $ContentID -= 1;
        }
        $Text=~s/CONTENT_ID/c_$ContentID/;
        $ContentID += 1;
        $Content_Counter += 1;
    }
    return $Text;
}

sub registerUsage($$)
{
    my ($TypeId, $LVer) = @_;
    $Class_Constructed{$LVer}{$TypeId} = 1;
    if(my $BaseId = $TypeInfo{$LVer}{$TypeId}{"BaseType"}) {
        $Class_Constructed{$LVer}{$BaseId} = 1;
    }
}

sub checkVoidMethod($)
{
    my $Method = $_[0];
    
    if($Method=~s/\)(.+)\Z/\)V/g) {
        return $Method;
    }
    
    return undef;
}

sub detectAdded()
{
    foreach my $Method (keys(%{$MethodInfo{2}}))
    {
        if(not defined $MethodInfo{1}{$Method})
        {
            if(not methodFilter($Method, 2)) {
                next;
            }
            
            my $Class = getType($MethodInfo{2}{$Method}{"Class"}, 2);
            
            $CheckedTypes{$Class->{"Name"}} = 1;
            $CheckedMethods{$Method} = 1;
            
            if(not $MethodInfo{2}{$Method}{"Constructor"}
            and my $Overridden = findMethod($Method, 2, $Class->{"Name"}, 2))
            {
                if(defined $MethodInfo{1}{$Overridden}
                and $Class->{"Type"} eq "class"
                and ($TName_Tid{1}{$Class->{"Name"}} or $TName_Tid_Generic{1}{getGeneric($Class->{"Name"})}))
                { # class should exist in previous version
                    %{$CompatProblems{$Overridden}{"Class_Overridden_Method"}{"this.".getSFormat($Method)}}=(
                        "Type_Name"=>$Class->{"Name"},
                        "Target"=>$MethodInfo{2}{$Method}{"Signature"},
                        "Old_Value"=>$Overridden,
                        "New_Value"=>$Method);
                }
            }
            if($MethodInfo{2}{$Method}{"Abstract"}) {
                $AddedMethod_Abstract{$Class->{"Name"}}{$Method} = 1;
            }
            
            if(not ($MethodInfo{2}{$Method}{"Access"} eq "protected" and $Class->{"Final"})) {
                %{$CompatProblems{$Method}{"Added_Method"}{""}} = ();
            }
            
            if(not $MethodInfo{2}{$Method}{"Constructor"})
            {
                my $VoidMethod = checkVoidMethod($Method);
                my $ReturnType = getTypeName($MethodInfo{2}{$Method}{"Return"}, 2);
                
                if(defined $Class->{"GenericParam"}
                and defined $Class->{"GenericParam"}{$ReturnType}) {
                    $ReturnType = getTypeName($Class->{"GenericParam"}{$ReturnType}, 2);
                }
                
                if(defined $MethodInfo{1}{$VoidMethod}
                and $ReturnType ne "void")
                { # return value type changed from void
                    $ChangedReturnFromVoid{$VoidMethod} = 1;
                    $ChangedReturnFromVoid{$Method} = 1;
                    
                    %{$CompatProblems{$VoidMethod}{"Changed_Method_Return_From_Void"}{""}}=(
                        "New_Value"=>getTypeName($MethodInfo{2}{$Method}{"Return"}, 2)
                    );
                }
                elsif(my $OldMethod = $OldMethodSignature{getBaseSignature($Method)})
                {
                    if($OldMethod ne $Method)
                    {
                        my $OldReturnType = getTypeName($MethodInfo{1}{$OldMethod}{"Return"}, 1);
                        
                        %{$CompatProblems{$OldMethod}{"Changed_Method_Return"}{""}}=(
                            "Old_Value"=>$OldReturnType,
                            "New_Value"=>$ReturnType
                        );
                    }
                }
            }
        }
    }
}

sub detectRemoved()
{
    foreach my $Method (keys(%{$MethodInfo{1}}))
    {
        if(not defined $MethodInfo{2}{$Method})
        {
            if(not methodFilter($Method, 1)) {
                next;
            }
            
            my $Class = getType($MethodInfo{1}{$Method}{"Class"}, 1);
            
            $CheckedTypes{$Class->{"Name"}} = 1;
            $CheckedMethods{$Method} = 1;
            
            if(not $MethodInfo{1}{$Method}{"Constructor"}
            and my $MovedUp = findMethod($Method, 1, $Class->{"Name"}, 2))
            {
                if($Class->{"Type"} eq "class"
                and not $MethodInfo{1}{$Method}{"Abstract"}
                and ($TName_Tid{2}{$Class->{"Name"}} or $TName_Tid_Generic{2}{getGeneric($Class->{"Name"})}))
                {# class should exist in newer version
                    %{$CompatProblems{$Method}{"Class_Method_Moved_Up_Hierarchy"}{"this.".getSFormat($MovedUp)}}=(
                        "Type_Name"=>$Class->{"Name"},
                        "Target"=>$MethodInfo{2}{$MovedUp}{"Signature"},
                        "Old_Value"=>$Method,
                        "New_Value"=>$MovedUp);
                }
            }
            else
            {
                if($MethodInfo{1}{$Method}{"Abstract"}) {
                    $RemovedMethod_Abstract{$Class->{"Name"}}{$Method} = 1;
                }
                
                if(not ($MethodInfo{1}{$Method}{"Access"} eq "protected" and $Class->{"Final"})) {
                    %{$CompatProblems{$Method}{"Removed_Method"}{""}} = ();
                }
            }
        }
    }
}

sub getArchivePaths($$)
{
    my ($Dest, $LVer) = @_;
    if(-f $Dest) {
        return ($Dest);
    }
    elsif(-d $Dest)
    {
        $Dest=~s/[\/\\]+\Z//g;
        next if(not $Dest);
        
        my @Archives = ();
        foreach my $Path (cmdFind($Dest, "", "*\\.jar"))
        {
            next if(ignorePath($Path, $Dest));
            push(@Archives, realpath_F($Path));
        }
        return @Archives;
    }
    return ();
}

sub isCyclical($$) {
    return (grep {$_ eq $_[1]} @{$_[0]});
}

sub mergeAPIs($$)
{
    my ($LVer, $Dep) = @_;
    
    foreach my $TId (keys(%{$Dep->{"TypeInfo"}}))
    {
        $TypeInfo{$LVer}{$TId} = $Dep->{"TypeInfo"}{$TId};
        $TypeInfo{$LVer}{$TId}{"Dep"} = 1;
    }
    
    my $MInfo = $Dep->{"MethodInfo"};
    foreach my $M_Id (keys(%{$MInfo}))
    {
        if(my $Name = $MInfo->{$M_Id}{"Name"})
        {
            $MethodInfo{$LVer}{$Name} = $MInfo->{$M_Id};
            $MethodInfo{$LVer}{$Name}{"Dep"} = 1;
        }
    }
}

sub readAPIDump($$$)
{
    my ($LVer, $Path, $Subj) = @_;
    
    if(not $In::Opt{"CountMethods"}) {
        printMsg("INFO", "Reading API dump ($LVer) ...");
    }
    
    my $FilePath = "";
    if(isDump_U($Path))
    { # input *.dump
        $FilePath = $Path;
    }
    else
    { # input *.dump.tar.gz
        $FilePath = unpackDump($Path);
        if(not isDump_U($FilePath)) {
            exitStatus("Invalid_Dump", "specified API dump \'$Path\' is not valid, try to recreate it");
        }
    }
    
    my $APIRef = {};
    
    open(DUMP, $FilePath);
    local $/ = undef;
    my $Content = <DUMP>;
    close(DUMP);
    
    if(getDirname($FilePath) eq $In::Opt{"Tmp"}."/unpack")
    { # remove temp file
        unlink($FilePath);
    }
    
    if($Content!~/};\s*\Z/) {
        exitStatus("Invalid_Dump", "specified API dump \'$Path\' is not valid, try to recreate it");
    }
    
    $APIRef = eval($Content);
    
    if(not $APIRef) {
        exitStatus("Error", "internal error - eval() procedure seem to not working correctly, try to remove 'use strict' and try again");
    }
    
    my $APIVer = $APIRef->{"API_DUMP_VERSION"};
    
    if($APIVer)
    {
        if(cmpVersions($APIVer, $API_DUMP_VERSION)>0)
        { # future formats
            exitStatus("Dump_Version", "version of the API dump is newer than version of the tool");
        }
        
        if(cmpVersions($APIVer, $API_DUMP_VERSION)<0)
        { # old formats
            printMsg("WARNING", "version of the API dump is older than version of the tool");
        }
    }
    
    if(cmpVersions($APIVer, $API_DUMP_VERSION_MIN)<0)
    { # obsolete formats
        exitStatus("Dump_Version", "version of the API dump is too old and unsupported anymore, please regenerate it");
    }
    
    if($Subj ne "Dep")
    {
        $In::Desc{$LVer}{"Version"} = $APIRef->{"LibraryVersion"};
        $In::Desc{$LVer}{"Dump"} = 1;
    }
    
    return $APIRef;
}

sub checkVersionNum($$)
{
    my ($LVer, $Path) = @_;
    
    if($In::Desc{$LVer}{"TargetVersion"}) {
        return;
    }
    
    if($Path!~/\.jar\Z/i) {
        return;
    }
    
    my $Ver = undef;
    
    if(not defined $Ver) {
        $Ver = getManifestVersion(getAbsPath($Path));
    }
    
    if(not defined $Ver) {
        $Ver = getPkgVersion(getFilename($Path));
    }
    
    if(not defined $Ver) {
        $Ver = parseVersion($Path);
    }
    
    if(not defined $Ver)
    {
        if($In::Opt{"DumpAPI"})
        {
            $Ver = "XYZ";
        }
        else
        {
            if($LVer==1) {
                $Ver = "X";
            }
            else {
                $Ver = "Y";
            }
        }
    }
    
    $In::Desc{$LVer}{"TargetVersion"} = $Ver;
    
    if($In::Opt{"DumpAPI"}) {
        printMsg("WARNING", "set version number to $Ver (use -vnum option to change it)");
    }
    else {
        printMsg("WARNING", "set #$LVer version number to $Ver (use --v$LVer=NUM option to change it)");
    }
}

sub getManifestVersion($)
{
    my $Path = $_[0];
    
    my $JarCmd = getCmdPath("jar");
    if(not $JarCmd) {
        exitStatus("Not_Found", "can't find \"jar\" command");
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    chdir($TmpDir);
    system($JarCmd." -xf \"$Path\" META-INF 2>null");
    chdir($In::Opt{"OrigDir"});
    
    my $Manifest = $TmpDir."/META-INF/MANIFEST.MF";
    
    if(-f $Manifest)
    {
        if(my $Content = readFile($Manifest))
        {
            if($Content=~/(\A|\s)Implementation\-Version:\s*(.+)(\s|\Z)/i) {
                return $2;
            }
        }
    }
    return undef;
}

sub parseVersion($)
{
    my $Str = $_[0];
    
    if(not $Str) {
        return undef;
    }
    
    if($Str=~/(\/|\\|\w|\A)[\-\_]*(\d+[\d\.\-]+\d+|\d+)/) {
        return $2;
    }
    return undef;
}

sub getPkgVersion($)
{
    my $Name = $_[0];
    $Name=~s/\.\w+\Z//;
    if($Name=~/\A(.+[a-z])[\-\_](v|ver|)(\d.+?)\Z/i)
    { # libsample-N
      # libsample-vN
        return ($1, $3);
    }
    elsif($Name=~/\A(.+?)(\d[\d\.]*)\Z/i)
    { # libsampleN
        return ($1, $2);
    }
    elsif($Name=~/\A(.+)[\-\_](v|ver|)(\d.+?)\Z/i)
    { # libsample-N
      # libsample-vN
        return ($1, $3);
    }
    elsif($Name=~/\A([a-z_\-]+)(\d.+?)\Z/i)
    { # libsampleNb
        return ($1, $2);
    }
    return (undef, undef);
}

sub dumpSorting($)
{
    my $Hash = $_[0];
    return [] if(not $Hash);
    my @Keys = keys(%{$Hash});
    return [] if($#Keys<0);
    if($Keys[0]=~/\A\d+\Z/)
    { # numbers
        return [sort {int($a)<=>int($b)} @Keys];
    }
    else
    { # strings
        return [sort {$a cmp $b} @Keys];
    }
}

sub printStatMsg($)
{
    my $Level = $_[0];
    printMsg("INFO", "Total ".lc($Level)." compatibility problems: ".$RESULT{$Level}{"Problems"}.", warnings: ".$RESULT{$Level}{"Warnings"});
}

sub printReport()
{
    printMsg("INFO", "Creating compatibility report ...");
    createReport();
    if($In::Opt{"JoinReport"} or $In::Opt{"DoubleReport"})
    {
        if($RESULT{"Binary"}{"Problems"}
        or $RESULT{"Source"}{"Problems"})
        {
            printMsg("INFO", "Binary compatibility: ".(100-$RESULT{"Binary"}{"Affected"})."\%");
            printMsg("INFO", "Source compatibility: ".(100-$RESULT{"Source"}{"Affected"})."\%");
        }
        else
        {
            printMsg("INFO", "Binary compatibility: 100\%");
            printMsg("INFO", "Source compatibility: 100\%");
        }
        printStatMsg("Binary");
        printStatMsg("Source");
    }
    elsif($In::Opt{"BinaryOnly"})
    {
        if($RESULT{"Binary"}{"Problems"}) {
            printMsg("INFO", "Binary compatibility: ".(100-$RESULT{"Binary"}{"Affected"})."\%");
        }
        else {
            printMsg("INFO", "Binary compatibility: 100\%");
        }
        printStatMsg("Binary");
    }
    elsif($In::Opt{"SourceOnly"})
    {
        if($RESULT{"Source"}{"Problems"}) {
            printMsg("INFO", "Source compatibility: ".(100-$RESULT{"Source"}{"Affected"})."\%");
        }
        else {
            printMsg("INFO", "Source compatibility: 100\%");
        }
        printStatMsg("Source");
    }
    if($In::Opt{"JoinReport"})
    {
        printMsg("INFO", "Report: ".getReportPath("Join"));
    }
    elsif($In::Opt{"DoubleReport"})
    { # default
        printMsg("INFO", "Report (BC): ".getReportPath("Binary"));
        printMsg("INFO", "Report (SC): ".getReportPath("Source"));
    }
    elsif($In::Opt{"BinaryOnly"})
    { # --binary
        printMsg("INFO", "Report: ".getReportPath("Binary"));
    }
    elsif($In::Opt{"SourceOnly"})
    { # --source
        printMsg("INFO", "Report: ".getReportPath("Source"));
    }
}

sub getReportPath($)
{
    my $Level = $_[0];
    my $Dir = "compat_reports/".$In::Opt{"TargetLib"}."/".$In::Desc{1}{"Version"}."_to_".$In::Desc{2}{"Version"};
    if($Level eq "Binary")
    {
        if($In::Opt{"BinaryReportPath"})
        { # --bin-report-path
            return $In::Opt{"BinaryReportPath"};
        }
        elsif($In::Opt{"OutputReportPath"})
        { # --report-path
            return $In::Opt{"OutputReportPath"};
        }
        else
        { # default
            return $Dir."/bin_compat_report.html";
        }
    }
    elsif($Level eq "Source")
    {
        if($In::Opt{"SourceReportPath"})
        { # --src-report-path
            return $In::Opt{"SourceReportPath"};
        }
        elsif($In::Opt{"OutputReportPath"})
        { # --report-path
            return $In::Opt{"OutputReportPath"};
        }
        else
        { # default
            return $Dir."/src_compat_report.html";
        }
    }
    else
    {
        if($In::Opt{"OutputReportPath"})
        { # --report-path
            return $In::Opt{"OutputReportPath"};
        }
        else
        { # default
            return $Dir."/compat_report.html";
        }
    }
}

sub unpackDump($)
{
    my $Path = $_[0];
    
    if(isDump_U($Path)) {
        return $Path;
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    $Path = getAbsPath($Path);
    $Path = pathFmt($Path);
    
    my ($Dir, $FileName) = sepPath($Path);
    my $UnpackDir = $TmpDir."/unpack";
    if(-d $UnpackDir) {
        rmtree($UnpackDir);
    }
    mkpath($UnpackDir);
    
    if($FileName=~s/\Q.zip\E\Z//g)
    { # *.zip
        my $UnzipCmd = getCmdPath("unzip");
        if(not $UnzipCmd) {
            exitStatus("Not_Found", "can't find \"unzip\" command");
        }
        chdir($UnpackDir);
        system("$UnzipCmd \"$Path\" >contents.txt");
        chdir($In::Opt{"OrigDir"});
        if($?) {
            exitStatus("Error", "can't extract \'$Path\'");
        }
        
        my @Contents = ();
        foreach (split("\n", readFile("$UnpackDir/contents.txt")))
        {
            if(/inflating:\s*([^\s]+)/) {
                push(@Contents, $1);
            }
        }
        if(not @Contents) {
            exitStatus("Error", "can't extract \'$Path\'");
        }
        return join_P($UnpackDir, $Contents[0]);
    }
    elsif($FileName=~s/\Q.tar.gz\E\Z//g)
    { # *.tar.gz
        if($In::Opt{"OS"} eq "windows")
        { # -xvzf option is not implemented in tar.exe (2003)
          # use "gzip.exe -k -d -f" + "tar.exe -xvf" instead
            my $TarCmd = getCmdPath("tar");
            if(not $TarCmd) {
                exitStatus("Not_Found", "can't find \"tar\" command");
            }
            my $GzipCmd = getCmdPath("gzip");
            if(not $GzipCmd) {
                exitStatus("Not_Found", "can't find \"gzip\" command");
            }
            chdir($UnpackDir);
            qx/$GzipCmd -k -d -f "$Path"/; # keep input files (-k)
            if($?) {
                exitStatus("Error", "can't extract \'$Path\'");
            }
            my @Contents = qx/$TarCmd -xvf "$Dir\\$FileName.tar"/;
            chdir($In::Opt{"OrigDir"});
            if($? or not @Contents) {
                exitStatus("Error", "can't extract \'$Path\'");
            }
            unlink($Dir."/".$FileName.".tar");
            chomp $Contents[0];
            return join_P($UnpackDir, $Contents[0]);
        }
        else
        { # Linux, Unix, OS X
            my $TarCmd = getCmdPath("tar");
            if(not $TarCmd) {
                exitStatus("Not_Found", "can't find \"tar\" command");
            }
            chdir($UnpackDir);
            my @Contents = qx/$TarCmd -xvzf "$Path" 2>&1/;
            chdir($In::Opt{"OrigDir"});
            if($? or not @Contents) {
                exitStatus("Error", "can't extract \'$Path\'");
            }
            $Contents[0]=~s/^x //; # OS X
            chomp $Contents[0];
            return join_P($UnpackDir, $Contents[0]);
        }
    }
}

sub createArchive($$)
{
    my ($Path, $To) = @_;
    if(not $To) {
        $To = ".";
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    my ($From, $Name) = sepPath($Path);
    if($In::Opt{"OS"} eq "windows")
    { # *.zip
        my $ZipCmd = getCmdPath("zip");
        if(not $ZipCmd) {
            exitStatus("Not_Found", "can't find \"zip\"");
        }
        my $Pkg = $To."/".$Name.".zip";
        unlink($Pkg);
        chdir($To);
        system("$ZipCmd -j \"$Name.zip\" \"$Path\" >\"$TmpDir/null\"");
        if($?)
        { # cannot allocate memory (or other problems with "zip")
            chdir($In::Opt{"OrigDir"});
            exitStatus("Error", "can't pack the API dump: ".$!);
        }
        chdir($In::Opt{"OrigDir"});
        unlink($Path);
        return $Pkg;
    }
    else
    { # *.tar.gz
        my $TarCmd = getCmdPath("tar");
        if(not $TarCmd) {
            exitStatus("Not_Found", "can't find \"tar\"");
        }
        my $GzipCmd = getCmdPath("gzip");
        if(not $GzipCmd) {
            exitStatus("Not_Found", "can't find \"gzip\"");
        }
        my $Pkg = abs_path($To)."/".$Name.".tar.gz";
        if(-e $Pkg) {
            unlink($Pkg);
        }
        system($TarCmd, "-C", $From, "-czf", $Pkg, $Name);
        if($?)
        { # cannot allocate memory (or other problems with "tar")
            exitStatus("Error", "can't pack the API dump: ".$!);
        }
        unlink($Path);
        return $To."/".$Name.".tar.gz";
    }
}

sub initAliases($)
{
    my $LVer = $_[0];
    
    initAPI($LVer);
    
    $MethodInfo{$LVer} = $In::API{$LVer}{"MethodInfo"};
    $TypeInfo{$LVer} = $In::API{$LVer}{"TypeInfo"};
    $TName_Tid{$LVer} = $In::API{$LVer}{"TName_Tid"};
    
    initAliases_TypeAttr($LVer);
}

sub createAPIFile($$)
{
    my ($LVer, $DescPath) = @_;
    
    if(not -e $DescPath) {
        exitStatus("Access_Error", "can't access \'$DescPath\'");
    }
    
    detectDefaultPaths("bin", "java");
    
    if(isDump($DescPath))
    {
        $In::API{$LVer} = readAPIDump($LVer, $DescPath, "Main");
        initAliases($LVer);
        
        if(my $V = $In::Desc{$LVer}{"TargetVersion"}) {
            $In::Desc{$LVer}{"Version"} = $V;
        }
        else {
            $In::Desc{$LVer}{"Version"} = $In::API{$LVer}{"LibraryVersion"};
        }
    }
    else
    {
        loadModule("APIDump");
    
        checkVersionNum($LVer, $DescPath);
        readDesc(createDesc($DescPath, $LVer), $LVer);
        
        initLogging($LVer);
        
        createAPIDump($LVer);
    }
    
    if(my $Dep = $In::Desc{$LVer}{"DepDump"}) {
        mergeAPIs($LVer, readAPIDump($LVer, $Dep, "Dep"));
    }
    
    printMsg("INFO", "Creating library API dump ...");
    
    $In::API{$LVer}{"API_DUMP_VERSION"} = $API_DUMP_VERSION;
    $In::API{$LVer}{"JAPI_COMPLIANCE_CHECKER_VERSION"} = $TOOL_VERSION;
    
    foreach ("TName_Tid") {
        delete($In::API{$LVer}{$_});
    }
    
    my $DumpPath = "api_dumps/".$In::Opt{"TargetLib"}."/".$In::Desc{$LVer}{"Version"}."/API.dump";
    if($In::Opt{"OutputDumpPath"})
    { # user defined path
        $DumpPath = $In::Opt{"OutputDumpPath"};
    }
    
    my $ArExt = $In::Opt{"Ar"};
    my $Archive = ($DumpPath=~s/\Q.$ArExt\E\Z//g);
    
    if($Archive)
    {
        my $TarCmd = getCmdPath("tar");
        if(not $TarCmd) {
            exitStatus("Not_Found", "can't find \"tar\"");
        }
        my $GzipCmd = getCmdPath("gzip");
        if(not $GzipCmd) {
            exitStatus("Not_Found", "can't find \"gzip\"");
        }
    }
    
    my ($DDir, $DName) = sepPath($DumpPath);
    my $DPath = $In::Opt{"Tmp"}."/".$DName;
    if(not $Archive) {
        $DPath = $DumpPath;
    }
    
    mkpath($DDir);
    
    open(DUMP, ">", $DPath) || die ("can't open file \'$DPath\': $!\n");
    print DUMP Dumper($In::API{$LVer});
    close(DUMP);
    
    if(not -s $DPath) {
        exitStatus("Error", "can't create API dump because something is going wrong with the Data::Dumper module");
    }
    
    if($Archive) {
        $DumpPath = createArchive($DPath, $DDir);
    }
    
    if($In::Opt{"OutputDumpPath"}) {
        printMsg("INFO", "Dump path: ".$In::Opt{"OutputDumpPath"});
    }
    else {
        printMsg("INFO", "Dump path: $DumpPath");
    }
    exit(0);
}

sub compareInit()
{
    if(not $In::Desc{1}{"Path"}) {
        exitStatus("Error", "-old option is not specified");
    }
    if(not -e $In::Desc{1}{"Path"}) {
        exitStatus("Access_Error", "can't access \'".$In::Desc{1}{"Path"}."\'");
    }
    if(not $In::Desc{2}{"Path"}) {
        exitStatus("Error", "-new option is not specified");
    }
    if(not -e $In::Desc{2}{"Path"}) {
        exitStatus("Access_Error", "can't access \'".$In::Desc{2}{"Path"}."\'");
    }
    
    if($In::Opt{"Quick"})
    {
        $CompatRules{"Binary"}{"Interface_Added_Super_Interface"}{"Severity"} = "Low";
        $CompatRules{"Binary"}{"Abstract_Class_Added_Super_Abstract_Class"}{"Severity"} = "Low";
        $CompatRules{"Binary"}{"Abstract_Class_Added_Super_Interface"}{"Severity"} = "Low";
        $CompatRules{"Binary"}{"Abstract_Class_Added_Abstract_Method"}{"Severity"} = "Low";
        $CompatRules{"Binary"}{"Interface_Added_Abstract_Method"}{"Severity"} = "Low";
    }
    
    printMsg("INFO", "Preparing, please wait ...");
    
    detectDefaultPaths("bin", undef);
    
    if(isDump($In::Desc{1}{"Path"}))
    {
        $In::API{1} = readAPIDump(1, $In::Desc{1}{"Path"}, "Main");
        initAliases(1);
        
        if(my $V = $In::Desc{1}{"TargetVersion"}) {
            $In::Desc{1}{"Version"} = $V;
        }
        else {
            $In::Desc{1}{"Version"} = $In::API{1}{"LibraryVersion"};
        }
    }
    else
    {
        loadModule("APIDump");
        
        checkVersionNum(1, $In::Desc{1}{"Path"});
        readDesc(createDesc($In::Desc{1}{"Path"}, 1), 1);
        
        initLogging(1);
        detectDefaultPaths(undef, "java");
        createAPIDump(1);
    }
    
    if(my $Dep = $In::Desc{1}{"DepDump"}) {
        mergeAPIs(1, readAPIDump(1, $Dep, "Dep"));
    }
    
    if(isDump($In::Desc{2}{"Path"}))
    {
        $In::API{2} = readAPIDump(2, $In::Desc{2}{"Path"}, "Main");
        initAliases(2);
        
        if(my $V = $In::Desc{2}{"TargetVersion"}) {
            $In::Desc{2}{"Version"} = $V;
        }
        else {
            $In::Desc{2}{"Version"} = $In::API{2}{"LibraryVersion"};
        }
    }
    else
    {
        loadModule("APIDump");
        
        checkVersionNum(2, $In::Desc{2}{"Path"});
        readDesc(createDesc($In::Desc{2}{"Path"}, 2), 2);
        
        initLogging(2);
        detectDefaultPaths(undef, "java");
        createAPIDump(2);
    }
    
    if(my $Dep = $In::Desc{2}{"DepDump"}) {
        mergeAPIs(2, readAPIDump(2, $Dep, "Dep"));
    }
    
    prepareData(1);
    prepareData(2);
    
    foreach my $Inv (keys(%{$MethodUsed{2}}))
    {
        foreach my $M (keys(%{$MethodUsed{2}{$Inv}}))
        {
            my $InvType = $MethodUsed{2}{$Inv}{$M};
            
            if($InvType ne "static"
            and index($Inv, "<init>")==-1)
            {
                my $CName = $Inv;
                $CName=~s/\A\"\[L(.+);"/$1/g;
                $CName=~s/#/./g;
                
                if($CName=~/\A(.+?)\./)
                {
                    $CName = $1;
                    if($CName!~/\"/)
                    {
                        $CName=~s!/!.!g;
                        $ClassMethod_AddedUsed{$CName}{$Inv} = $M;
                    }
                }
            }
            
            if(not defined $MethodInfo{1}{$M}) {
                delete($MethodUsed{2}{$Inv}{$M});
            }
        }
    }
    
    foreach my $ClassName (keys(%ClassMethod_AddedUsed))
    {
        foreach my $MethodName (keys(%{$ClassMethod_AddedUsed{$ClassName}}))
        {
            if(defined $MethodInfo{1}{$MethodName}
            or defined $MethodInfo{2}{$MethodName}
            or defined $MethodUsed{1}{$MethodName}
            or findMethod($MethodName, 2, $ClassName, 1))
            { # abstract method added by the new super-class (abstract) or super-interface
                delete($ClassMethod_AddedUsed{$ClassName}{$MethodName});
            }
        }
        if(not keys(%{$ClassMethod_AddedUsed{$ClassName}})) {
            delete($ClassMethod_AddedUsed{$ClassName});
        }
    }
}

sub scenario()
{
    setTarget("default");
    
    initAliases(1);
    initAliases(2);
    
    $In::Opt{"OrigDir"} = cwd();
    $In::Opt{"Tmp"} = tempdir(CLEANUP=>1);
    $In::Opt{"Reproducible"} = 1;
    
    $In::Opt{"JoinReport"} = 1;
    $In::Opt{"DoubleReport"} = 0;
    
    if($In::Opt{"BinaryOnly"} and $In::Opt{"SourceOnly"})
    { # both --binary and --source
      # is the default mode
        $In::Opt{"DoubleReport"} = 1;
        $In::Opt{"JoinReport"} = 0;
        $In::Opt{"BinaryOnly"} = 0;
        $In::Opt{"SourceOnly"} = 0;
        if($In::Opt{"OutputReportPath"})
        { # --report-path
            $In::Opt{"DoubleReport"} = 0;
            $In::Opt{"JoinReport"} = 1;
        }
    }
    elsif($In::Opt{"BinaryOnly"} or $In::Opt{"SourceOnly"})
    { # --binary or --source
        $In::Opt{"DoubleReport"} = 0;
        $In::Opt{"JoinReport"} = 0;
    }
    if(defined $In::Opt{"Help"})
    {
        helpMsg();
        exit(0);
    }
    if(defined $In::Opt{"ShowVersion"})
    {
        printMsg("INFO", "Java API Compliance Checker (JAPICC) $TOOL_VERSION\nCopyright (C) 2017 Andrey Ponomarenko's ABI Laboratory\nLicense: LGPL or GPL <http://www.gnu.org/licenses/>\nThis program is free software: you can redistribute it and/or modify it.\n\nWritten by Andrey Ponomarenko.");
        exit(0);
    }
    if(defined $In::Opt{"DumpVersion"})
    {
        printMsg("INFO", $TOOL_VERSION);
        exit(0);
    }
    $Data::Dumper::Sortkeys = 1;
    
    # FIXME: can't pass \&dumpSorting - cause a segfault sometimes
    if($In::Opt{"SortDump"})
    {
        $Data::Dumper::Useperl = 1;
        $Data::Dumper::Sortkeys = \&dumpSorting;
    }
    
    if(defined $In::Opt{"TestTool"})
    {
        detectDefaultPaths("bin", "java");
        loadModule("RegTests");
        testTool();
        exit(0);
    }
    
    if(defined $In::Opt{"ShortMode"})
    {
        if(not defined $In::Opt{"AffectLimit"}) {
            $In::Opt{"AffectLimit"} = 10;
        }
    }
    
    if(not $In::Opt{"TargetLib"} and not $In::Opt{"CountMethods"})
    {
        if($In::Opt{"DumpAPI"})
        {
            if($In::Opt{"DumpAPI"}=~/\.jar\Z/)
            { # short usage
                my ($Name, $Version) = getPkgVersion(getFilename($In::Opt{"DumpAPI"}));
                if($Name and $Version ne "")
                {
                    $In::Opt{"TargetLib"} = $Name;
                    if(not $In::Desc{1}{"TargetVersion"}) {
                        $In::Desc{1}{"TargetVersion"} = $Version;
                    }
                }
            }
        }
        else
        {
            if($In::Desc{1}{"Path"}=~/\.jar\Z/ and $In::Desc{2}{"Path"}=~/\.jar\Z/)
            { # short usage
                my ($Name1, $Version1) = getPkgVersion(getFilename($In::Desc{1}{"Path"}));
                my ($Name2, $Version2) = getPkgVersion(getFilename($In::Desc{2}{"Path"}));
                
                if($Name1 and $Version1 ne ""
                and $Version2 ne "")
                {
                    $In::Opt{"TargetLib"} = $Name1;
                    if(not $In::Desc{1}{"TargetVersion"}) {
                        $In::Desc{1}{"TargetVersion"} = $Version1;
                    }
                    if(not $In::Desc{2}{"TargetVersion"}) {
                        $In::Desc{2}{"TargetVersion"} = $Version2;
                    }
                }
            }
        }
        
        if(not $In::Opt{"TargetLib"}) {
            exitStatus("Error", "library name is not selected (option --lib=NAME)");
        }
    }
    else
    { # validate library name
        if($In::Opt{"TargetLib"}=~/[\*\/\\]/) {
            exitStatus("Error", "\"\\\", \"\/\" and \"*\" symbols are not allowed in the library name");
        }
    }
    if(not $In::Opt{"TargetTitle"}) {
        $In::Opt{"TargetTitle"} = $In::Opt{"TargetLib"};
    }
    if(my $ClassListPath = $In::Opt{"ClassListPath"})
    {
        if(not -f $ClassListPath) {
            exitStatus("Access_Error", "can't access file \'$ClassListPath\'");
        }
        foreach my $Class (split(/\n/, readFile($ClassListPath)))
        {
            $Class=~s/\//./g;
            $In::Opt{"ClassList_User"}{$Class} = 1;
        }
    }
    if(my $AnnotationsListPath = $In::Opt{"AnnotationsListPath"})
    {
        if(not -f $AnnotationsListPath) {
            exitStatus("Access_Error", "can't access file \'$AnnotationsListPath\'");
        }
        foreach my $Annotation (split(/\n/, readFile($AnnotationsListPath)))
        {
            $In::Opt{"AnnotationList_User"}{$Annotation} = 1;
        }
    }
    if(my $SkipAnnotationsListPath = $In::Opt{"SkipAnnotationsListPath"})
    {
        if(not -f $SkipAnnotationsListPath) {
            exitStatus("Access_Error", "can't access file \'$SkipAnnotationsListPath\'");
        }
        foreach my $Annotation (split(/\n/, readFile($SkipAnnotationsListPath)))
        {
            $In::Opt{"SkipAnnotationList_User"}{$Annotation} = 1;
        }
    }
    if(my $SkipClassesList = $In::Opt{"SkipClassesList"})
    {
        if(not -f $SkipClassesList) {
            exitStatus("Access_Error", "can't access file \'$SkipClassesList\'");
        }
        foreach my $Class (split(/\n/, readFile($SkipClassesList)))
        {
            $Class=~s/\//./g;
            $In::Opt{"SkipClasses"}{$Class} = 1;
        }
    }
    if(my $SkipPackagesList = $In::Opt{"SkipPackagesList"})
    {
        if(not -f $SkipPackagesList) {
            exitStatus("Access_Error", "can't access file \'$SkipPackagesList\'");
        }
        foreach my $Package (split(/\n/, readFile($SkipPackagesList)))
        {
            $In::Desc{1}{"SkipPackages"}{$Package} = 1;
            $In::Desc{2}{"SkipPackages"}{$Package} = 1;
        }
    }
    if(my $ClientPath = $In::Opt{"ClientPath"})
    {
        if($ClientPath=~/\.class\Z/) {
            exitStatus("Error", "input file is not a java archive");
        }
        
        if(-f $ClientPath)
        {
            detectDefaultPaths("bin", undef);
            loadModule("APIDump");
            
            readArchive(0, $ClientPath)
        }
        else {
            exitStatus("Access_Error", "can't access file \'$ClientPath\'");
        }
    }
    
    if(my $DPath = $In::Opt{"CountMethods"})
    {
        if(not -e $DPath) {
            exitStatus("Access_Error", "can't access \'$DPath\'");
        }
        
        $In::API{1} = readAPIDump(1, $DPath, "Main");
        initAliases(1);
        
        my $Count = 0;
        foreach my $Method (keys(%{$MethodInfo{1}}))
        {
            $Count += methodFilter($Method, 1);
        }
        
        printMsg("INFO", $Count);
        exit(0);
    }
    
    if($In::Opt{"DumpAPI"})
    {
        createAPIFile(1, $In::Opt{"DumpAPI"});
        exit(0);
    }
    
    compareInit();
    
    readRules("Binary");
    readRules("Source");
    
    detectAdded();
    detectRemoved();
    
    printMsg("INFO", "Comparing classes ...");
    mergeClasses();
    mergeMethods();
    
    printReport();
    
    if($RESULT{"Source"}{"Problems"} + $RESULT{"Binary"}{"Problems"}) {
        exit(getErrorCode("Incompatible"));
    }
    else {
        exit(getErrorCode("Compatible"));
    }
}

scenario();

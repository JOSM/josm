###########################################################################
# A module to find system files
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

my %Cache;

my %DefaultBinPaths;

my %OS_AddPath=(
"macos"=>{
    "bin"=>{"/Developer/usr/bin"=>1}},
"beos"=>{
    "bin"=>{"/boot/common/bin"=>1,"/boot/system/bin"=>1,"/boot/develop/abi"=>1}}
);

sub getCmdPath($)
{
    my $Name = $_[0];
    
    if(defined $Cache{"getCmdPath"}{$Name}) {
        return $Cache{"getCmdPath"}{$Name};
    }
    my $Path = searchCmd($Name);
    if(not $Path and $In::Opt{"OS"} eq "windows")
    { # search for *.exe file
        $Path = searchCmd($Name.".exe");
    }
    if (not $Path) {
        $Path = searchCmd_Path($Name);
    }
    if($Path=~/\s/) {
        $Path = "\"".$Path."\"";
    }
    return ($Cache{"getCmdPath"}{$Name} = $Path);
}

sub searchCmd($)
{
    my $Name = $_[0];
    
    if(defined $Cache{"searchCmd"}{$Name}) {
        return $Cache{"searchCmd"}{$Name};
    }
    if(my $JdkPath = $In::Opt{"JdkPath"})
    {
        if(-x $JdkPath."/".$Name) {
            return ($Cache{"searchCmd"}{$Name} = $JdkPath."/".$Name);
        }
        
        if(-x $JdkPath."/bin/".$Name) {
            return ($Cache{"searchCmd"}{$Name} = $JdkPath."/bin/".$Name);
        }
    }
    if(my $DefaultPath = getCmdPath_Default($Name)) {
        return ($Cache{"searchCmd"}{$Name} = $DefaultPath);
    }
    return ($Cache{"searchCmd"}{$Name} = "");
}

sub searchCmd_Path($)
{
    my $Name = $_[0];
    
    if(defined $Cache{"searchCmd_Path"}{$Name}) {
        return $Cache{"searchCmd_Path"}{$Name};
    }
    
    if(defined $In::Opt{"SysPaths"}{"bin"})
    {
        foreach my $Path (sort {length($a)<=>length($b)} keys(%{$In::Opt{"SysPaths"}{"bin"}}))
        {
            if(-f $Path."/".$Name or -f $Path."/".$Name.".exe") {
                return ($Cache{"searchCmd_Path"}{$Name} = join_P($Path,$Name));
            }
        }
    }

    return ($Cache{"searchCmd_Path"}{$Name} = "");
}

sub getCmdPath_Default($)
{ # search in PATH
    if(defined $Cache{"getCmdPath_Default"}{$_[0]}) {
        return $Cache{"getCmdPath_Default"}{$_[0]};
    }
    return ($Cache{"getCmdPath_Default"}{$_[0]} = getCmdPath_Default_I($_[0]));
}

sub getCmdPath_Default_I($)
{ # search in PATH
    my $Name = $_[0];
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    if($Name=~/find/)
    { # special case: search for "find" utility
        if(`find \"$TmpDir\" -maxdepth 0 2>\"$TmpDir/null\"`) {
            return "find";
        }
    }
    if(getVersion($Name)) {
        return $Name;
    }
    if($In::Opt{"OS"} eq "windows")
    {
        if(`$Name /? 2>\"$TmpDir/null\"`) {
            return $Name;
        }
    }
    if($Name!~/which/)
    {
        if(my $WhichCmd = getCmdPath("which"))
        {
            if(`$WhichCmd $Name 2>\"$TmpDir/null\"`) {
                return $Name;
            }
        }
    }
    foreach my $Path (sort {length($a)<=>length($b)} keys(%DefaultBinPaths))
    {
        if(-f $Path."/".$Name) {
            return join_P($Path,$Name);
        }
    }
    return "";
}

sub detectDefaultPaths($$)
{
    my ($Bin, $Java) = @_;
    
    if($Cache{"detectDefaultPaths"}{$Bin}{$Java})
    { # enter once
        return;
    }
    $Cache{"detectDefaultPaths"}{$Bin}{$Java} = 1;
    
    if(not keys(%{$In::Opt{"SysPaths"}}))
    { # run once
        foreach my $Type (keys(%{$OS_AddPath{$In::Opt{"OS"}}}))
        { # additional search paths
            foreach my $Path (keys(%{$OS_AddPath{$In::Opt{"OS"}}{$Type}}))
            {
                next if(not -d $Path);
                $In::Opt{"SysPaths"}{$Type}{$Path} = $OS_AddPath{$In::Opt{"OS"}}{$Type}{$Path};
            }
        }
        if($In::Opt{"OS"} ne "windows")
        {
            foreach my $Type ("include", "lib", "bin")
            { # autodetecting system "devel" directories
                foreach my $Path (cmdFind("/", "d", "*$Type*", 1)) {
                    $In::Opt{"SysPaths"}{$Type}{$Path} = 1;
                }
                if(-d "/usr")
                {
                    foreach my $Path (cmdFind("/usr", "d", "*$Type*", 1)) {
                        $In::Opt{"SysPaths"}{$Type}{$Path} = 1;
                    }
                }
            }
        }
    }
    
    if($Bin)
    {
        detectBinDefaultPaths();
        foreach my $Path (keys(%DefaultBinPaths)) {
            $In::Opt{"SysPaths"}{"bin"}{$Path} = $DefaultBinPaths{$Path};
        }
    }
    
    if($Java)
    {
        if(my $JavacCmd = getCmdPath("javac"))
        {
            if(my $Ver = `$JavacCmd -version 2>&1`)
            {
                if($Ver=~/javac\s+(.+)/)
                {
                    printMsg("INFO", "Using Java ".$1);
                    $In::Opt{"CompilerVer"} = $1;
                }
            }
        }
    }
}

sub detectBinDefaultPaths()
{
    my $EnvPaths = $ENV{"PATH"};
    if($In::Opt{"OS"} eq "beos") {
        $EnvPaths .= ":".$ENV{"BETOOLS"};
    }
    elsif($In::Opt{"OS"} eq "windows"
    and my $JHome = $ENV{"JAVA_HOME"}) {
        $EnvPaths .= ";$JHome\\bin";
    }
    my $Sep = ($In::Opt{"OS"} eq "windows")?";":":|;";
    foreach my $Path (sort {length($a)<=>length($b)} split(/$Sep/, $EnvPaths))
    {
        $Path=~s/[\/\\]+\Z//g;
        if($Path) {
            $DefaultBinPaths{$Path} = 1;
        }
    }
}

return 1;

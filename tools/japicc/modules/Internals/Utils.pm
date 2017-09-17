###########################################################################
# A module with basic functions
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
use POSIX;

sub initAPI($)
{
    my $V = $_[0];
    foreach my $K ("MethodInfo", "TypeInfo", "TName_Tid")
    {
        if(not defined $In::API{$V}{$K}) {
            $In::API{$V}{$K} = {};
        }
    }
}

sub setTarget($)
{
    my $Target = $_[0];
    
    if($Target eq "default")
    {
        $Target = getOSgroup();
        
        $In::Opt{"OS"} = $Target;
        $In::Opt{"Ar"} = getArExt($Target);
    }
    
    $In::Opt{"Target"} = $Target;
}

sub getMaxLen()
{
    if($In::Opt{"OS"} eq "windows") {
        return 8191;
    }
    
    return undef;
}

sub getMaxArg()
{
    if($In::Opt{"OS"} eq "windows") {
        return undef;
    }
    
    # Linux
    # return POSIX::sysconf(POSIX::_SC_ARG_MAX);
    # javap failed on rt.jar (GC triggered before VM initialization completed)
    return 10000;
}

sub divideArray($)
{
    my $ArrRef = $_[0];
    
    return () if($#{$ArrRef}==-1);
    
    my $LEN_MAX = getMaxLen();
    my $ARG_MAX = getMaxArg();
    
    if(defined $ARG_MAX)
    { # Linux
        if($#{$ArrRef} < $ARG_MAX - 500) {
            return $ArrRef;
        }
    }
    
    my @Res = ();
    my $Sub = [];
    my $Len = 0;
    
    foreach my $Pos (0 .. $#{$ArrRef})
    {
        my $Arg = $ArrRef->[$Pos];
        my $Arg_L = length($Arg) + 1; # space
        
        my ($LenLimit, $ArgLimit) = (1, 1);
        
        if(defined $LEN_MAX) {
            $LenLimit = ($Len < $LEN_MAX - 500);
        }
        
        if(defined $ARG_MAX) {
            $ArgLimit = ($#{$Sub} < $ARG_MAX - 500);
        }
        
        if($LenLimit and $ArgLimit)
        {
            push(@{$Sub}, $Arg);
            $Len += $Arg_L;
        }
        else
        {
            push(@Res, $Sub);
            
            $Sub = [$Arg];
            $Len = $Arg_L;
        }
    }
    
    if($#{$Sub}!=-1) {
        push(@Res, $Sub);
    }
    
    return @Res;
}

sub cmdFind(@)
{ # native "find" is much faster than File::Find (~6x)
  # also the File::Find doesn't support --maxdepth N option
  # so using the cross-platform wrapper for the native one
    my ($Path, $Type, $Name, $MaxDepth, $UseRegex) = ();
    
    $Path = shift(@_);
    if(@_) {
        $Type = shift(@_);
    }
    if(@_) {
        $Name = shift(@_);
    }
    if(@_) {
        $MaxDepth = shift(@_);
    }
    if(@_) {
        $UseRegex = shift(@_);
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    my $TmpFile = $TmpDir."/null";
    
    if($In::Opt{"OS"} eq "windows")
    {
        $Path = getAbsPath($Path);
        my $Cmd = "cmd /C dir \"$Path\" /B /O";
        if($MaxDepth!=1) {
            $Cmd .= " /S";
        }
        if($Type eq "d") {
            $Cmd .= " /AD";
        }
        elsif($Type eq "f") {
            $Cmd .= " /A-D";
        }
        
        my @Files = split(/\n/, qx/$Cmd/);
        
        if($Name)
        {
            if(not $UseRegex)
            { # FIXME: how to search file names in MS shell?
              # wildcard to regexp
                $Name=~s/\*/.*/g;
                $Name='\A'.$Name.'\Z';
            }
            @Files = grep { /$Name/i } @Files;
        }
        my @AbsPaths = ();
        foreach my $File (@Files)
        {
            if(not isAbsPath($File)) {
                $File = join_P($Path, $File);
            }
            if($Type eq "f" and not -f $File)
            { # skip dirs
                next;
            }
            push(@AbsPaths, $File);
        }
        if($Type eq "d") {
            push(@AbsPaths, $Path);
        }
        return @AbsPaths;
    }
    else
    {
        my $FindCmd = "find";
        if(not checkCmd($FindCmd)) {
            exitStatus("Not_Found", "can't find a \"find\" command");
        }
        $Path = getAbsPath($Path);
        if(-d $Path and -l $Path
        and $Path!~/\/\Z/)
        { # for directories that are symlinks
            $Path.="/";
        }
        my $Cmd = $FindCmd." \"$Path\"";
        if($MaxDepth) {
            $Cmd .= " -maxdepth $MaxDepth";
        }
        if($Type) {
            $Cmd .= " -type $Type";
        }
        if($Name and not $UseRegex)
        { # wildcards
            $Cmd .= " -name \"$Name\"";
        }
        my $Res = qx/$Cmd 2>"$TmpFile"/;
        if($? and $!) {
            printMsg("ERROR", "problem with \'find\' utility ($?): $!");
        }
        my @Files = split(/\n/, $Res);
        if($Name and $UseRegex)
        { # regex
            @Files = grep { /$Name/ } @Files;
        }
        return @Files;
    }
}

sub getVersion($)
{
    my $Cmd = $_[0];
    my $TmpDir = $In::Opt{"Tmp"};
    my $Ver = `$Cmd --version 2>\"$TmpDir/null\"`;
    return $Ver;
}

return 1;

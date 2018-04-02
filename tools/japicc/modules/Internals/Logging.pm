###########################################################################
# A module for logging
#
# Copyright (C) 2016-2018 Andrey Ponomarenko's ABI Laboratory
#
# Written by Andrey Ponomarenko
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
# MA  02110-1301  USA.
###########################################################################
use strict;

my %DEBUG_DIR;

my %ERROR_CODE = (
    # Compatible verdict
    "Compatible"=>0,
    "Success"=>0,
    # Incompatible verdict
    "Incompatible"=>1,
    # Undifferentiated error code
    "Error"=>2,
    # System command is not found
    "Not_Found"=>3,
    # Cannot access input files
    "Access_Error"=>4,
    # Invalid input API dump
    "Invalid_Dump"=>7,
    # Incompatible version of API dump
    "Dump_Version"=>8,
    # Cannot find a module
    "Module_Error"=>9
);

sub exitStatus($$)
{
    my ($Code, $Msg) = @_;
    print STDERR "ERROR: ". $Msg."\n";
    if(my $Orig = $In::Opt{"OrigDir"}) {
        chdir($Orig);
    }
    exit($ERROR_CODE{$Code});
}

sub getErrorCode($) {
    return $ERROR_CODE{$_[0]};
}

sub printMsg($$)
{
    my ($Type, $Msg) = @_;
    if($Type!~/\AINFO/) {
        $Msg = $Type.": ".$Msg;
    }
    if($Type!~/_C\Z/) {
        $Msg .= "\n";
    }
    if($Type eq "ERROR") {
        print STDERR $Msg;
    }
    else {
        print $Msg;
    }
}

sub initLogging($)
{
    my $LVer = $_[0];
    if($In::Opt{"Debug"})
    { # debug directory
        $DEBUG_DIR{$LVer} = "debug/".$In::Opt{"TargetLib"}."/".$In::Desc{$LVer}{"Version"};
        
        if(-d $DEBUG_DIR{$LVer}) {
            rmtree($DEBUG_DIR{$LVer});
        }
    }
}

sub getDebugDir($) {
    return $DEBUG_DIR{$_[0]};
}

return 1;

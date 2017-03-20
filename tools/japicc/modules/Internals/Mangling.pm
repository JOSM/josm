###########################################################################
# A module to unmangle symbols
#
# Copyright (C) 2016 Andrey Ponomarenko's ABI Laboratory
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

sub unmangle($)
{
    my $Name = $_[0];
    
    $Name=~s!/!.!g;
    $Name=~s!:\(!(!g;
    $Name=~s!\).+\Z!)!g;
    
    if($Name=~/\A(.+)\((.+)\)/)
    {
        my ($ShortName, $MangledParams) = ($1, $2);
        my @UnmangledParams = ();
        my ($IsArray, $Shift, $Pos, $CurParam) = (0, 0, 0, "");
        while($Pos<length($MangledParams))
        {
            my $Symbol = substr($MangledParams, $Pos, 1);
            if($Symbol eq "[")
            { # array
                $IsArray = 1;
                $Pos+=1;
            }
            elsif($Symbol eq "L")
            { # class
                if(substr($MangledParams, $Pos+1)=~/\A(.+?);/) {
                    $CurParam = $1;
                    $Shift = length($CurParam)+2;
                }
                if($IsArray) {
                    $CurParam .= "[]";
                }
                $Pos+=$Shift;
                push(@UnmangledParams, $CurParam);
                ($IsArray, $Shift, $CurParam) = (0, 0, "")
            }
            else
            {
                if($Symbol eq "C") {
                    $CurParam = "char";
                }
                elsif($Symbol eq "B") {
                    $CurParam = "byte";
                }
                elsif($Symbol eq "S") {
                    $CurParam = "short";
                }
                elsif($Symbol eq "S") {
                    $CurParam = "short";
                }
                elsif($Symbol eq "I") {
                    $CurParam = "int";
                }
                elsif($Symbol eq "F") {
                    $CurParam = "float";
                }
                elsif($Symbol eq "J") {
                    $CurParam = "long";
                }
                elsif($Symbol eq "D") {
                    $CurParam = "double";
                }
                else {
                    printMsg("INFO", "WARNING: unmangling error");
                }
                if($IsArray) {
                    $CurParam .= "[]";
                }
                $Pos+=1;
                push(@UnmangledParams, $CurParam);
                ($IsArray, $Shift, $CurParam) = (0, 0, "")
            }
        }
        return $ShortName."(".join(", ", @UnmangledParams).")";
    }
    
    return $Name;
}

return 1;

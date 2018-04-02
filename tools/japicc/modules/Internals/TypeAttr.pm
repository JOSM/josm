###########################################################################
# A module to handle type attributes
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

my %Cache;

#Aliases
my %TypeInfo = ();

sub initAliases_TypeAttr($)
{
    my $LVer = $_[0];
    
    $TypeInfo{$LVer} = $In::API{$LVer}{"TypeInfo"};
}

sub getTypeName($$)
{
    my ($TypeId, $LVer) = @_;
    return $TypeInfo{$LVer}{$TypeId}{"Name"};
}

sub getShortName($$)
{
    my ($TypeId, $LVer) = @_;
    my $TypeName = $TypeInfo{$LVer}{$TypeId}{"Name"};
    $TypeName=~s/\A.*\.//g;
    return $TypeName;
}

sub getTypeType($$)
{
    my ($TypeId, $LVer) = @_;
    return $TypeInfo{$LVer}{$TypeId}{"Type"};
}

sub getBaseType($$)
{
    my ($TypeId, $LVer) = @_;
    
    if(defined $Cache{"getBaseType"}{$TypeId}{$LVer}) {
        return $Cache{"getBaseType"}{$TypeId}{$LVer};
    }
    if(not $TypeInfo{$LVer}{$TypeId}) {
        return {};
    }
    my $Type = $TypeInfo{$LVer}{$TypeId};
    if(not $Type->{"BaseType"}) {
        return $Type;
    }
    $Type = getBaseType($Type->{"BaseType"}, $LVer);
    return ($Cache{"getBaseType"}{$TypeId}{$LVer} = $Type);
}

sub getOneStepBaseType($$)
{
    my ($TypeId, $LVer) = @_;
    
    if(not $TypeInfo{$LVer}{$TypeId}) {
        return {};
    }
    
    my $Type = $TypeInfo{$LVer}{$TypeId};
    if(not $Type->{"BaseType"}) {
        return $Type;
    }
    return getType($Type->{"BaseType"}, $LVer);
}

sub getType($$)
{
    my ($TypeId, $LVer) = @_;
    if(not $TypeId or not $TypeInfo{$LVer}{$TypeId}) {
        return {};
    }
    return $TypeInfo{$LVer}{$TypeId};
}

sub getGeneric($)
{
    my $Name = $_[0];
    $Name=~s/<.*>//g;
    return $Name;
}

return 1;

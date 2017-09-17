###########################################################################
# A module to filter API symbols
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

my %SkippedPackage;

sub classFilter($$$)
{
    my ($Class, $LVer, $ClassContext) = @_;
    
    if(defined $Class->{"Dep"}) {
        return 0;
    }
    
    my $CName = $Class->{"Name"};
    my $Package = $Class->{"Package"};
    
    if(defined $In::Opt{"ClassListPath"}
    and not defined $In::Opt{"ClassList_User"}{$CName})
    { # user defined classes
        return 0;
    }
    
    if(defined $In::Opt{"SkipClassesList"})
    { # user defined classes
        if(defined $In::Opt{"SkipClasses"}{$CName}) {
            return 0;
        }
        else
        {
            my $SClass = $CName;
            
            while($SClass=~s/\.[^\.]+?\Z//)
            {
                if(defined $In::Opt{"SkipClasses"}{$SClass}) {
                    return 0;
                }
            }
        }
    }
    
    if(skipPackage($Package, $LVer))
    { # internal packages
        return 0;
    }
    
    if(skipType($CName))
    { # internal types
        return 0;
    }
    
    if($ClassContext)
    {
        my @Ann = ();
        
        if(defined $Class->{"Annotations"}) {
            @Ann = keys(%{$Class->{"Annotations"}});
        }
        
        if(not annotationFilter(\@Ann, $LVer)) {
            return 0;
        }
        
        if($In::Opt{"ClientPath"})
        {
            if(not defined $In::Opt{"UsedClasses_Client"}{$CName}) {
                return 0;
            }
        }
    }
    
    return 1;
}

sub methodFilter($$)
{
    my ($Method, $LVer) = @_;
    
    my $MAttr = $In::API{$LVer}{"MethodInfo"}{$Method};
    my $MName = $MAttr->{"Name"};
    
    if(defined $MAttr->{"Dep"}) {
        return 0;
    }
    
    if($MAttr->{"Access"}=~/private/)
    { # non-public
        return 0;
    }
    
    my $ClassId = $MAttr->{"Class"};
    my $Class = getType($ClassId, $LVer);
    
    if($Class->{"Access"}=~/private/)
    { # skip private classes
        return 0;
    }
    
    my $Package = $MAttr->{"Package"};
    
    my @Ann = ();
    
    if(defined $Class->{"Annotations"}) {
        @Ann = (@Ann, keys(%{$Class->{"Annotations"}}));
    }
    
    if(defined $MAttr->{"Annotations"}) {
        @Ann = (@Ann, keys(%{$MAttr->{"Annotations"}}));
    }
    
    if(not annotationFilter(\@Ann, $LVer)) {
        return 0;
    }
    
    if($In::Opt{"ClientPath"})
    { # user defined application
        if(not defined $In::Opt{"UsedMethods_Client"}{$MName}
        and not defined $In::Opt{"UsedClasses_Client"}{$Class->{"Name"}}) {
            return 0;
        }
    }
    
    if(skipPackage($Package, $LVer))
    { # internal packages
        return 0;
    }
    
    if(not classFilter($Class, $LVer, 0)) {
        return 0;
    }
    
    if(defined $In::Opt{"SkipDeprecated"})
    {
        if($Class->{"Deprecated"})
        { # deprecated class
            return 0;
        }
        if($MAttr->{"Deprecated"})
        { # deprecated method
            return 0;
        }
    }
    
    return 1;
}

sub annotationFilter($$)
{
    my ($Ann, $LVer) = @_;
    
    if(not defined $In::Opt{"CountMethods"})
    {
        if(defined $In::Opt{"AddedAnnotations"} and $LVer==1) {
            return 1;
        }
        
        if(defined $In::Opt{"RemovedAnnotations"} and $LVer==2) {
            return 1;
        }
    }
    
    if($In::Opt{"SkipAnnotationsListPath"})
    {
        foreach my $Aid (@{$Ann})
        {
            my $AName = $In::API{$LVer}{"TypeInfo"}{$Aid}{"Name"};
            
            if(defined $In::Opt{"SkipAnnotationList_User"}{$AName}) {
                return 0;
            }
        }
    }
    
    if($In::Opt{"AnnotationsListPath"})
    {
        my $Annotated = 0;
        
        foreach my $Aid (@{$Ann})
        {
            my $AName = $In::API{$LVer}{"TypeInfo"}{$Aid}{"Name"};
            
            if(defined $In::Opt{"AnnotationList_User"}{$AName})
            {
                $Annotated = 1;
                last;
            }
        }
        
        if(not $Annotated) {
            return 0;
        }
    }
    
    return 1;
}

sub skipType($)
{
    my $TName = $_[0];
    
    if(my $SkipInternalTypes = $In::Opt{"SkipInternalTypes"})
    {
        if($TName=~/($SkipInternalTypes)/) {
            return 1;
        }
    }
    
    return 0;
}

sub skipPackage($$)
{
    my ($Package, $LVer) = @_;
    
    if(my $SkipInternalPackages = $In::Opt{"SkipInternalPackages"})
    {
        if($Package=~/($SkipInternalPackages)/) {
            return 1;
        }
    }
    
    if(defined $In::Desc{$LVer}{"SkipPackages"})
    {
        foreach my $P (keys(%{$In::Desc{$LVer}{"SkipPackages"}}))
        {
            if($Package=~/\A\Q$P\E(\.|\Z)/)
            { # user skipped packages
                return 1;
            }
        }
    }
    
    if(not defined $In::Opt{"KeepInternal"})
    {
        my $Note = (not keys(%SkippedPackage));
        
        if($Package=~/(\A|\.)(internal|impl|examples)(\.|\Z)/)
        { # internal packages
            my $P = $2;
            if(not $SkippedPackage{$LVer}{$P})
            {
                $SkippedPackage{$LVer}{$P} = 1;
                printMsg("WARNING", "skipping \"$P\" packages");
                
                if($Note) {
                    printMsg("NOTE", "use --keep-internal option to check them");
                }
            }
            return 1;
        }
    }
    
    if(defined $In::Desc{$LVer}{"KeepPackages"}
    and my @Keeped = keys(%{$In::Desc{$LVer}{"KeepPackages"}}))
    {
        my $UserKeeped = 0;
        foreach my $P (@Keeped)
        {
            if($Package=~/\A\Q$P\E(\.|\Z)/)
            { # user keeped packages
                $UserKeeped = 1;
                last;
            }
        }
        if(not $UserKeeped) {
            return 1;
        }
    }
    
    if(my $Check = $In::Opt{"CheckPackages"})
    {
        if($Package!~/$Check/) {
            return 1;
        }
    }
    
    return 0;
}

sub ignorePath($$)
{
    my ($Path, $Prefix) = @_;
    
    if($Path=~/\~\Z/)
    { # skipping system backup files
        return 1;
    }
    
    # skipping hidden .svn, .git, .bzr, .hg and CVS directories
    if(cutPrefix($Path, $Prefix)=~/(\A|[\/\\]+)(\.(svn|git|bzr|hg)|CVS)([\/\\]+|\Z)/)
    {
        return 1;
    }
    
    return 0;
}

return 1;

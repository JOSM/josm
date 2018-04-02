###########################################################################
# A module with simple functions
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
use Digest::MD5 qw(md5_hex);
use File::Spec::Functions qw(abs2rel);
use Config;

my %Cache;

my $MD5_LEN = 12;

sub getOSgroup()
{
    my $N = $Config{"osname"};
    my $G = undef;
    
    if($N=~/macos|darwin|rhapsody/i) {
        $G = "macos";
    }
    elsif($N=~/freebsd|openbsd|netbsd/i) {
        $G = "bsd";
    }
    elsif($N=~/haiku|beos/i) {
        $G = "beos";
    }
    elsif($N=~/symbian|epoc/i) {
        $G = "symbian";
    }
    elsif($N=~/win/i) {
        $G = "windows";
    }
    elsif($N=~/solaris/i) {
        $G = "solaris";
    }
    else
    { # linux, unix-like
        $G = "linux";
    }
    
    return $G;
}

sub getArExt($)
{
    my $Target = $_[0];
    if($Target eq "windows") {
        return "zip";
    }
    return "tar.gz";
}

sub getMd5(@)
{
    my $Md5 = md5_hex(@_);
    return substr($Md5, 0, $MD5_LEN);
}

sub writeFile($$)
{
    my ($Path, $Content) = @_;
    
    if(my $Dir = getDirname($Path)) {
        mkpath($Dir);
    }
    open (FILE, ">".$Path) || die ("can't open file \'$Path\': $!\n");
    print FILE $Content;
    close(FILE);
}

sub readFile($)
{
    my $Path = $_[0];
    
    open (FILE, $Path);
    my $Content = join("", <FILE>);
    close(FILE);
    
    $Content=~s/\r//g;
    
    return $Content;
}

sub appendFile($$)
{
    my ($Path, $Content) = @_;
    
    if(my $Dir = getDirname($Path)) {
        mkpath($Dir);
    }
    open(FILE, ">>".$Path) || die ("can't open file \'$Path\': $!\n");
    print FILE $Content;
    close(FILE);
}

sub readLineNum($$)
{
    my ($Path, $Num) = @_;
    
    open (FILE, $Path);
    foreach (1 ... $Num) {
        <FILE>;
    }
    my $Line = <FILE>;
    close(FILE);
    
    return $Line;
}

sub readAttributes($$)
{
    my ($Path, $Num) = @_;
    
    my %Attributes = ();
    if(readLineNum($Path, $Num)=~/<!--\s+(.+)\s+-->/)
    {
        foreach my $AttrVal (split(/;/, $1))
        {
            if($AttrVal=~/(.+):(.+)/)
            {
                my ($Name, $Value) = ($1, $2);
                $Attributes{$Name} = $Value;
            }
        }
    }
    return \%Attributes;
}

sub getFilename($)
{ # much faster than basename() from File::Basename module
    if(defined $Cache{"getFilename"}{$_[0]}) {
        return $Cache{"getFilename"}{$_[0]};
    }
    if($_[0] and $_[0]=~/([^\/\\]+)[\/\\]*\Z/) {
        return ($Cache{"getFilename"}{$_[0]}=$1);
    }
    return ($Cache{"getFilename"}{$_[0]}="");
}

sub getDirname($)
{ # much faster than dirname() from File::Basename module
    if(defined $Cache{"getDirname"}{$_[0]}) {
        return $Cache{"getDirname"}{$_[0]};
    }
    if($_[0] and $_[0]=~/\A(.*?)[\/\\]+[^\/\\]*[\/\\]*\Z/) {
        return ($Cache{"getDirname"}{$_[0]}=$1);
    }
    return ($Cache{"getDirname"}{$_[0]}="");
}

sub sepPath($) {
    return (getDirname($_[0]), getFilename($_[0]));
}

sub checkCmd($)
{
    my $Cmd = $_[0];

    foreach my $Path (sort {length($a)<=>length($b)} split(/:/, $ENV{"PATH"}))
    {
        if(-x $Path."/".$Cmd) {
            return 1;
        }
    }
    
    return 0;
}

sub isAbsPath($) {
    return ($_[0]=~/\A(\/|\w+:[\/\\])/);
}

sub cutPrefix($$)
{
    my ($Path, $Prefix) = @_;
    $Prefix=~s/[\/\\]+\Z//;
    $Path=~s/\A\Q$Prefix\E([\/\\]+|\Z)//;
    return $Path;
}

sub showPos($)
{
    my $N = $_[0];
    if(not $N) {
        $N = 1;
    }
    else {
        $N = int($N)+1;
    }
    if($N>3) {
        return $N."th";
    }
    elsif($N==1) {
        return "1st";
    }
    elsif($N==2) {
        return "2nd";
    }
    elsif($N==3) {
        return "3rd";
    }
    
    return $N;
}

sub parseTag($$)
{
    my ($CodeRef, $Tag) = @_;
    
    if(${$CodeRef}=~s/\<\Q$Tag\E\>((.|\n)+?)\<\/\Q$Tag\E\>//)
    {
        my $Content = $1;
        $Content=~s/(\A\s+|\s+\Z)//g;
        return $Content;
    }
    
    return "";
}

sub isDump($)
{
    if($_[0]=~/\A(.+)\.(api|dump|apidump)(\Q.tar.gz\E|\Q.zip\E|)\Z/) {
        return $1;
    }
    return 0;
}

sub isDump_U($)
{
    if($_[0]=~/\.(api|dump|apidump)\Z/) {
        return 1;
    }
    return 0;
}

sub cmpVersions($$)
{ # compare two version strings in dotted-numeric format
    my ($V1, $V2) = @_;
    return 0 if($V1 eq $V2);
    my @V1Parts = split(/\./, $V1);
    my @V2Parts = split(/\./, $V2);
    for (my $i = 0; $i <= $#V1Parts && $i <= $#V2Parts; $i++)
    {
        return -1 if(int($V1Parts[$i]) < int($V2Parts[$i]));
        return 1 if(int($V1Parts[$i]) > int($V2Parts[$i]));
    }
    return -1 if($#V1Parts < $#V2Parts);
    return 1 if($#V1Parts > $#V2Parts);
    return 0;
}

sub getRelPath($$)
{
    my ($A, $B) = @_;
    return abs2rel($A, getDirname($B));
}

sub getPFormat($)
{
    my $Name = $_[0];
    $Name=~s/\//./g;
    return $Name;
}

return 1;

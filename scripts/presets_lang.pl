#! /usr/bin/perl -w
# short tool to update language links in presets file

use XML::Parser;
use Data::Dumper;
use URI::Escape;

my $skip = 0;
my $xml = new XML::Parser(Handlers => {Start => \&handle_start});
undef $/;
open FILE,"<","data/defaultpresets.xml" or die;
my $file = <FILE>;
close FILE;
$xml->parsefile('data/defaultpresets.xml');

sub handle_start
{
  my ($expat, $element, %data) = @_;

  if($element eq "link" && $skip < 50000)
  {
    my %datan;
    foreach my $k (sort {$az=$a;$bz=$b;$az=~s/.?href//;;$bz=~s/.?href//;$az cmp $bz} keys %data)
    {
      my $z = $k;
      $z =~ s/.?href//;
      #printf("%-5s %s\n", $z,$data{$k});
    }

    if(!$data{href})
    {
      print "Missing href: %s\n", join(" ", %data);
    }
    else
    {
      my $main = `wget "$data{href}" -O - -q`;
      my $h = uri_unescape($data{href});
      my $v = "href=\"$h\"";
      while($main =~ /<a href="(\/wiki\/[^"]+)"[^>]+>• <bdi lang="([a-z_A-Z-]+)"(?: style="unicode-bidi:embed;unicode-bidi:-webkit-isolate;unicode-bidi:isolate")?>/g)
      {
        my $lang = lc($2);
        my $val = uri_unescape($1);
        $lang = "$1_".uc($2) if($lang =~ /^(..)[_-](..)$/);
        $lang = "zh_CN" if $lang eq "zh-hans";
        $lang = "zh_TW" if $lang eq "zh-hant";
        if(-f "data/$lang.lang")
        {
          $datan{$lang} = $val;
        }
        else
        {
          print "Skip lang $lang\n";
        }
      }
      while($main =~ /<a href="(\/wiki\/([a-zA-Z-_]):.*?)".*?&#160;&#8226;&#160;/g)
      {
        my $lang = lc($1);
        my $val = uri_unescape($2);
        $lang = "$1_".uc($2) if($lang =~ /^(..)[_-](..)$/);
        $datan{$lang} = $val;
      }
      foreach my $l (sort keys %datan)
      {
        $v .= "\n" . (" " x 18) . "$l.href=\"https://wiki.openstreetmap.org$datan{$l}\"";
      }
      print "$v\n";
      print "Replace failed for $data{href}.\n" if !($file =~ s/(<link )href="\Q$data{href}\E".*?( ?\/>)/$1$v$2/s);

      $skip++;
    }
  }
}
open FILE,">","data/defaultpresets.xml" or die;
print FILE $file;
close FILE;

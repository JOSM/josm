#! /usr/bin/perl -w
# short tool to find out all used icons and allows deleting unused icons
# when building release files

my @default = (
  "styles/standard/*.xml",
  "styles/standard/*.mapcss",
  "data/*.xml",
  "src/org/openstreetmap/josm/*.java",
  "src/org/openstreetmap/josm/*/*.java",
  "src/org/openstreetmap/josm/*/*/*.java",
  "src/org/openstreetmap/josm/*/*/*/*.java",
  "src/org/openstreetmap/josm/*/*/*/*/*.java",
  "src/org/openstreetmap/josm/*/*/*/*/*/*.java"
);

my %icons;

my $o = $/;

for my $arg (@ARGV ? @ARGV : @default)
{
  for my $file (glob($arg))
  {
    open(FILE,"<",$file) or die "Could not open $file\n";
    #print "Read file $file\n";
    $/ = $file =~ /\.java$/ ? ";" : $o;
    my $extends = "";
    while(my $l = <FILE>)
    {
      if($l =~ /src\s*=\s*["'](.*?)["']/)
      {
        my $img = "styles/standard/$1";
        $img = "styles/$1" if((!-f "images/$img") && -f "images/styles/$1");
        $img = $1 if((!-f "images/$img") && -f "images/$1");
        ++$icons{$img};
      }
      elsif($l =~ /icon\s*[:=]\s*["']([^+]+?)["']/)
      {
        ++$icons{$1};
      }

      if($l =~ /icon-image:\s*\"?(.*?)\"?\s*;/)
      {
        my $img = "styles/standard/$1";
        $img = "styles/$1" if((!-f "images/$img") && -f "images/styles/$1");
        $img = $1 if((!-f "images/$img") && -f "images/$1");
        ++$icons{$img};
      }
      if($l =~ /ImageProvider\.get\(\"([^\"]*?)\"\)/)
      {
        my $i = $1;
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      while($l =~ /\/\*\s*ICON\s*\*\/\s*\"(.*?)\"/g)
      {
        my $i = $1;
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      while($l =~ /\/\*\s*ICON\((.*?)\)\s*\*\/\s*\"(.*?)\"/g)
      {
        my $i = "$1$2";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /new\s+ImageLabel\(\"(.*?)\"/)
      {
        my $i = "statusline/$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /createPreferenceTab\(\"(.*?)\"/)
      {
        my $i = "preferences/$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.get\(\"(.*?)\",\s*\"(.*?)\"\s*\)/)
      {
        my $i = "$1/$2";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.overlay\(.*?,\s*\"(.*?)\",/)
      {
        my $i = $1;
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /getCursor\(\"(.*?)\",\s*\"(.*?)\"/)
      {
        my $i = "cursor/modifier/$2";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
        $i = "cursor/$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.getCursor\(\"(.*?)\",\s*null\)/)
      {
        my $i = "cursor/$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /SideButton*\(\s*(?:mark)?tr\s*\(\s*\".*?\"\s*\)\s*,\s*\"(.*?)\"/)
      {
        my $i = "dialogs/$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /super\(\s*tr\(\".*?\"\),\s*\"(.*?)\"/s)
      {
        my $i = "$extends$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /super\(\s*trc\(\".*?\",\s*\".*?\"\),\s*\"(.*?)\"/s)
      {
        my $i = "$extends$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /audiotracericon\",\s*\"(.*?)\"/s)
      {
        my $i = "markers/$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /\"(.*?)\",\s*parentLayer/s)
      {
        my $i = "markers/$1";
        $i .= ".png" if !($i =~ /\.png$/);
        ++$icons{$i};
      }
      if($l =~ /allowedtypes\s+=.*\{(.*)\}/s)
      {
        my $t = $1;
        while($t =~ /\"(.*?)\"/g)
        {
          ++$icons{"Mf_$1.png"};
        }
      }
      if($l =~ /MODES\s+=.*\{(.*)\}/s)
      {
        my $t = $1;
        while($t =~ /\"(.*?)\"/g)
        {
          ++$icons{"dialogs/autoscale/$1.png"};
        }
      }
      if($l =~ /enum\s+DeleteMode\s*\{(.*)/s)
      {
        my $t = $1;
        while($t =~ /\"(.*?)\"/g)
        {
          ++$icons{"cursor/modifier/$1.png"};
        }
      }
      if($l =~ /\.setButtonIcons.*\{(.*)\}/)
      {
        my $t = $1;
        while($t =~ /\"(.*?)\"/g)
        {
          my $i = $1;
          $i .= ".png" if !($i =~ /\.png$/);
          ++$icons{$i};
        }
      }
      if($l =~ /extends MapMode/)
      {
        $extends = "mapmode/";
      }
      if($l =~ /extends ToggleDialog/)
      {
        $extends = "dialogs/";
      }
    }
    close FILE;
  }
}

my %haveicons;

for($i = 1; my @ifiles = glob("images".("/*" x $i).".png"); ++$i)
{
  for my $ifile (sort @ifiles)
  {
    $ifile =~ s/^images\///;
    $haveicons{$ifile} = 1;
  }
}

for my $img (sort keys %icons)
{
  print STDERR "File $img does not exist!\n" if(!-f "images/$img");
  delete $haveicons{$img};
}

for my $img (sort keys %haveicons)
{
  print "$img\n";
}

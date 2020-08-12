#! /usr/bin/perl -w
# short tool to find out all used icons and allows deleting unused icons
# when building release files

my @default = (
  "resources/styles/standard/*.xml",
  "resources/styles/standard/*.mapcss",
  "resources/data/*.xml",
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
    my @defs;
    open(FILE,"<",$file) or die "Could not open $file\n";
    #print "Read file $file\n";
    $/ = $file =~ /\.java$/ ? ";" : $o;
    my $extends = "";
    while(my $l = <FILE>)
    {
      if($l =~ /private static final String ([A-Z_]+) = ("[^"]+")/)
      {
        push(@defs, [$1, $2]);
      }
      next if $l =~ /NO-ICON/;
      for my $d (@defs)
      {
        $l =~ s/$d->[0]/$d->[1]/g;
      }
      if($l =~ /icon\s*[:=]\s*["']([^"'+]+?)["']/)
      {
        ++$icons{$1};
      }

      if(($l =~ /(?:icon-image|repeat-image|fill-image)\s*:\s*(\"?(.*?)\"?)\s*;/) && ($1 ne "none"))
      {
        my $img = $2;
        ++$icons{$img};
      }
      if($l =~ /ImageProvider(?:\.get)?\(\"([^\"]*?)\"(?:, (?:ImageProvider\.)?ImageSizes\.[A-Z]+)?\)/)
      {
        my $i = $1;
        ++$icons{$i};
      }
      while($l =~ /\/\*\s*ICON\s*\*\/\s*\"(.*?)\"/g)
      {
        my $i = $1;
        ++$icons{$i};
      }
      while($l =~ /\/\*\s*ICON\((.*?)\)\s*\*\/\s*\"(.*?)\"/g)
      {
        my $i = "$1$2";
        ++$icons{$i};
      }
      if($l =~ /new\s+ImageLabel\(\"(.*?)\"/)
      {
        my $i = "statusline/$1";
        ++$icons{$i};
      }
      if($l =~ /setIcon\(\"(.*?)\"/)
      {
        my $i = "statusline/$1";
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.get(?:IfAvailable)?\(\"(.*?)\",\s*\"(.*?)\"(?:, (?:ImageProvider\.)?ImageSizes\.[A-Z]+)?\s*\)/)
      {
        my $i = "$1/$2";
        ++$icons{$i};
      }
      if($l =~ /new ImageProvider\(\"(.*?)\",\s*\"(.*?)\"\s*\)/)
      {
        my $i = "$1/$2";
        ++$icons{$i};
      }
      if($l =~ /getCursor\(\"(.*?)\",\s*\"(.*?)\"/)
      {
        my $i = "cursor/modifier/$2";
        ++$icons{$i};
        $i = "cursor/$1";
        ++$icons{$i};
      }
      if($l =~ /ImageProvider\.getCursor\(\"(.*?)\",\s*null\)/)
      {
        my $i = "cursor/$1";
        ++$icons{$i};
      }
      if($l =~ /super\(\s*tr\(\".*?\"\),\s*\"(.*?)\"/s)
      {
        my $i = "$extends$1";
        ++$icons{$i};
      }
      if($l =~ /super\(\s*trc\(\".*?\",\s*\".*?\"\),\s*\"(.*?)\"/s)
      {
        my $i = "$extends$1";
        ++$icons{$i};
      }
      if($l =~ /setButtonIcons.*\{(.*)\}/ || $l =~ /setButtonIcons\((.*)\)/ )
      {
        my $t = $1;
        while($t =~ /\"(.*?)\"/g)
        {
          my $i = $1;
          ++$icons{$i};
        }
      }
      if($l =~ /extends MapMode/)
      {
        $extends = "mapmode/";
      }
      elsif($l =~ /extends ToggleDialog/)
      {
        $extends = "dialogs/";
      }
      elsif($l =~ /extends JosmAction/)
      {
        $extends = "";
      }
    }
    close FILE;
  }
}

my %haveicons;

for($i = 1; my @ifiles = (glob("resources/images".("/*" x $i).".png"), glob("resources/images".("/*" x $i).".svg")); ++$i)
{
  for my $ifile (sort @ifiles)
  {
    $ifile =~ s/^resources\/images\///;
    # svg comes after png due to the glob, so only check for svg's
    if($ifile =~ /^(.*)\.svg$/)
    {
      if($haveicons{"$1.png"})
      {
        print STDERR "$1: File exists twice as .svg and .png.\n";
      }
      # check for unwanted svg effects
      if(open FILE, "<","resources/images/$ifile")
      {
        undef $/;
        my $f = <FILE>;
        close FILE;
        for my $sep ("'", '"')
        {
          while($f =~ /style\s*=\s*$sep([^$sep]+)$sep/g)
          {
            for my $x (split(/\s*;\s*/, $1))
            {
              print STDERR "$ifile: Style starts with minus: $x\n" if $x =~ /^-/;
            }
          }
        }
        if($f =~ /<style[^>]+type=['"]text\/css['"][^>]*>/m)
        {
          print STDERR "$ifile: CSS-Style in SVG icon not supported\n";
        }
        if($f =~ /viewBox\s*=\s*["']([^"']+)["']/)
        {
          my $x = $1;
          print STDERR "$ifile: ViewBox has float values: $x\n" if $x =~ /\./;
        }
      }
      else
      {
        print STDERR "$ifile: Could not open file: $1";
      }
    }
    $haveicons{$ifile} = 1;
  }
}

for my $img (sort keys %icons)
{
  if($img =~ /\.(png|svg)/)
  {
    print STDERR "$img: File does not exist!\n" if(!-f "resources/images/$img");
    delete $haveicons{$img};
  }
  else
  {
    print STDERR "$img(.svg|.png): File does not exist!\n" if(!-f "resources/images/$img.png" && !-f "resources/images/$img.svg");
    delete $haveicons{"$img.svg"};
    delete $haveicons{"$img.png"};
  }
}

for my $img (sort keys %haveicons)
{
  print "$img: Unused image.\n";
}

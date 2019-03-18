#! /usr/bin/perl -w

use strict;
use utf8;
use open qw/:std :encoding(utf8)/;
use Net::HTTPS;
use XML::LibXML;

my %urls;
my %known;

sub getignores
{
  open FILE,"<:encoding(utf-8)","josm_httpsignores.txt" or die;
  for my $line (<FILE>)
  {
    if($line =~ /\|\| TestHTTPS \|\| \{\{\{(.*)\}\}\} \|\|/)
    {
      $known{$1} = 1;
    }
  }
  close FILE;
}

sub getmaps
{
  my $dom = XML::LibXML->load_xml(location => "imagery_josm.imagery.xml");
  my $xpc = XML::LibXML::XPathContext->new($dom);
  $xpc->registerNs('j',  'http://josm.openstreetmap.de/maps-1.0');
  foreach my $entry ($xpc->findnodes("//j:entry"))
  {
    my $name = $xpc->findvalue("./j:name", $entry);
    for my $e ($xpc->findnodes(".//j:*", $entry))
    {
      if($e->textContent =~ /^http:\/\/(.*?)[\/]/)
      {
        my $u = $1;
        if($u =~ /^(.*)\{switch:(.*)\}(.*)$/)
        {
          my ($f,$switch,$e) = ($1, $2, $3);
          for my $s (split(",", $switch))
          {
            $urls{"$f$s$e"}{"MAP:$name"}++;
          }
        }
        else
        {
          $urls{$u}{"MAP:$name"}++;
        }
      }
    }
  }
}

sub getfile($$)
{
  my ($type, $file) = @_;
  open FILE,"<:encoding(utf-8)",$file or die;
  my $name;
  for my $line (<FILE>)
  {
    if($line =~ /^([^ \t].*);(.*)/)
    {
      my ($n, $url) = ($1, $2);
      if($url =~ /josm\.openstreetmap\.de/)
      {
        $name = "WIKI$type:$n";
      }
      else
      {
        $name = "$type:$n";
      }
    }
    if($line =~ /http:\/\/(.*?)[\/]/)
    {
      $urls{$1}{$name}++;
    }
  }
  close FILE;
}

sub getdump()
{
  open FILE,"<:encoding(utf-8)","josm_dump.txt" or die;
  local $/;
  undef $/;
  my $file = <FILE>;
  close FILE;
  eval $file;
}

print "Options: \n PLUGIN STYLE RULE PRESET MAP DUMP\n GETPLUGIN GETSTYLE GETRULE GETPRESET GETMAP GETDUMP\n LOCAL\n IGNORES GETIGNORES\n ALL GETALL\n" if !@ARGV;

open OUTFILE,">","josm_https.txt" or die "Could not open output file";

sub doprint($)
{
  my $t = $_[0];
  for my $k (sort keys %known)
  {
    $known{$k}++ if($t =~ s/(\Q$k\E)/~~$1~~/g);
  }
  print OUTFILE $t;
  print $t;
}

my $local = 0;
for my $ARG (@ARGV)
{
  if($ARG eq "ALL") {push(@ARGV, "PLUGIN", "STYLE", "RULE", "PRESET", "MAP", "DUMP", "IGNORES");}
  if($ARG eq "GETALL") {push(@ARGV, "GETPLUGIN", "GETSTYLE", "GETRULE", "GETPRESET", "GETMAP", "GETDUMP", "GETIGNORES");}
}
my %ARGS = map {$_ => 1} @ARGV; # prevent double arguments by passing through a hash
for my $ARG (sort keys %ARGS)
{
  if($ARG eq "GETIGNORES") { system "curl https://josm.openstreetmap.de/wiki/IntegrationTestIgnores?format=txt -o josm_httpsignores.txt"; getignores();}
  if($ARG eq "IGNORES") { getignores(); }
  if($ARG eq "LOCAL") {$local = 1; }
  if($ARG eq "GETDUMP") { system "scp josm\@josm.openstreetmap.de:auto/httpinfo.dump josm_dump.txt"; getdump();}
  if($ARG eq "DUMP") { getdump(); }
  if($ARG eq "GETMAP") { system "curl https://josm.openstreetmap.de/maps -o imagery_josm.imagery.xml"; getmaps();}
  if($ARG eq "MAP") { getmaps(); }
  for my $x ("PLUGIN", "STYLE", "RULE", "PRESET")
  {
    my $t = lc($x);
    my $url = $x eq "PLUGIN" ? $t : "${t}s";
    my $file = "josm_$t.xml";
    if($ARG eq "GET$x") { system "curl https://josm.openstreetmap.de/$url -o $file"; getfile($x, $file);}
    if($ARG eq $x) { getfile($x, $file); }
  }
}

for my $url (sort keys %urls)
{
  my $i = join(" # ", sort keys %{$urls{$url}});
  if($local) # skip test
  {
    doprint "* $url:$i\n";
    next;
  }
  eval
  {
    local $SIG{ALRM} = sub {die "--Alarm--"};

    alarm(5);
    my $s = Net::HTTPS->new(Host => $url) || die $@;
    $s->write_request(GET => "/", 'User-Agent' => "TestHTTPS/1.0");
    my($code, $mess, %h) = $s->read_response_headers;
    alarm(0);
    doprint "* $url [$code $mess]: $i\n";
  };
  if($@ && $@ !~ "(--Alarm--|Connection refused)")
  {
    my $e = $@;
    $e =~ s/[\r\n]//g;
    $e =~ s/ at scripts\/TestHTTPS.pl .*//;
    doprint "* $url [Error $e] :$i\n";
  }
}

for my $k (sort keys %known)
{
  print "Unused ignores $k\n" if $known{$k} == 1;
}

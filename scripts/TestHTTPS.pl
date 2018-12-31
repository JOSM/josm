#! /usr/bin/perl -w

use strict;
use utf8;
use open qw/:std :encoding(utf8)/;
use Net::HTTPS;
use XML::LibXML;

my %urls;

my %known = map {$_ => 1} qw(
  siglon.londrina.pr.gov.br
  tiles.itoworld.com
  tms.cadastre.openstreetmap.fr
  www.jgoodies.com
  zibi.openstreetmap.org.pl
);

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
    if($line =~ /^([^ \t].*);/)
    {
      $name = $1;
    }
    if($line =~ /http:\/\/(.*?)[\/]/)
    {
      $urls{$1}{"$type:$name"}++;
    }
  }
}

print "Options: PLUGIN STYLE RULE PRESET MAP GETPLUGIN GETSTYLE GETRULE GETPRESET GETMAP LOCAL\n" if !@ARGV;

open OUTFILE,">>","josm_https.txt" or die "Could not open output file";

sub doprint($)
{
  print OUTFILE $_[0];
  print $_[0];
}

my $local = 0;
for my $ARG (@ARGV)
{
  if($ARG eq "LOCAL") {$local = 1; }
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
    doprint "* ".($known{$url} ? "~~" : "")."$url:$i\n";
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
    doprint "* ".($known{$url} ? "~~" : "")."$url [$code $mess]: $i\n";
  };
  if($@ && $@ !~ "(--Alarm--|Connection refused)")
  {
    my $e = $@;
    $e =~ s/[\r\n]//g;
    $e =~ s/ at scripts\/TestHTTPS.pl .*//;
    doprint "* ".($known{$url} ? "~~" : "")."$url [Error $e] :$i\n";
  }
}

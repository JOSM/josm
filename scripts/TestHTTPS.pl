#! /usr/bin/perl -w

use strict;
use utf8;
use open qw/:std :encoding(utf8)/;
use Net::HTTPS;

use XML::LibXML;

my $dom = XML::LibXML->load_xml(location => "imagery_josm.imagery.xml");
my $xpc = XML::LibXML::XPathContext->new($dom);
$xpc->registerNs('j',  'http://josm.openstreetmap.de/maps-1.0');
my %urls;

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
          $urls{"$f$s$e"}{$name}++;
        }
      }
      else
      {
        $urls{$u}{$name}++;
      }
    }
  }
}

for my $url (sort keys %urls)
{
  my $i = join(" # ", sort keys %{$urls{$url}});
  eval
  {
    local $SIG{ALRM} = sub {die "--Alarm--"};

    alarm(5);
    my $s = Net::HTTPS->new(Host => $url) || die $@;
    $s->write_request(GET => "/", 'User-Agent' => "TestHTTPS/1.0");
    my($code, $mess, %h) = $s->read_response_headers;
    alarm(0);
    print "* $url [$code $mess]: $i\n";
  };
  if($@ && $@ !~ "(--Alarm--|Connection refused)")
  {
    my $e = $@;
    $e =~ s/[\r\n]//g;
    $e =~ s/ at scripts\/TestHTTPS.pl .*//;
    print "* $url [Error $e] :$i\n";
  }
}

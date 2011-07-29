#!/usr/bin/perl

use strict;
use warnings;

print "# Reference data created by proj.4\n";
print "#\n";
print "# code,lat,lon,east,north\n";
for my $in (<>) {
    # input data looks like this: "EPSG:4326 Bounds[-90.0,-180.0,90.0,180.0]"
    # (created by ProjectionRefTest.java)
    next unless $in =~ /EPSG:([0-9]+) Bounds\[(.*),(.*),(.*),(.*)\]/;
    my ($epsg, $minlat, $minlon, $maxlat, $maxlon) = ($1, $2, $3, $4, $5);
    next if $epsg =~ /325.../;      # strange codes, don't seem to exist
    next if $epsg eq '4326';        # trivial, but annoying, because output isn't in meters
    next if $epsg =~ /^2756[1-4]$/; # proj.4 seems to give wrong results for Lambert 4 zones (missing grid shift file?)
    if ($epsg eq '3059') {          # proj.4 cannot handle the wider bounds that are built into josm
        ($minlat, $minlon, $maxlat, $maxlon) = (55.64,20.98,58.12,28.23);
    }
    #print "$epsg: ($minlat, $minlon, $maxlat, $maxlon)\n";
    
    for (1 .. 3) {
        my $lat = rand() * ($maxlat - $minlat) + $minlat;
        my $lon = rand() * ($maxlon - $minlon) + $minlon;
        
        open PROJ4, "echo \"$lon $lat\" | cs2cs +init=epsg:4326 +to +init=epsg:$epsg -f %.9f |" or die;
        my $res = <PROJ4>;
        die unless $res =~ /(\S+)\s+(\S+)\s/;
        print "EPSG:$epsg,$lat,$lon,$1,$2\n"; 
        close PROJ4 or die "error: $! $?";
    }
    
}

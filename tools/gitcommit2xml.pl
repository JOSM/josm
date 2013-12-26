#!perl -w

$l=<>;
if ($l =~ /([-+: .\w]+)/) {
    $date=$1;
}

while (<>) {
    if (/git-svn-id: [^@]*@([0-9]+)\s/) {
        $rev=$1;
    }
}
print "<info><entry><commit". ($rev?" revision=\"$rev\"":"") .">". ($date?"<date>$date</date>":"") ."</commit></entry></info>\n";

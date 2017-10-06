#!/usr/bin/python
# License: CC0

"""
Helper script to replace "@since xxx" in Javadoc by the upcoming revision number.

Will retrieve the current revision number from the server. It runs over all
modified and added .java files and replaces xxx in "since xxx" by the revision
number that is to be expected for the next commit.
"""

import xml.etree.ElementTree as ElementTree
import subprocess
import re

revision = None

def main():
    svn_status = subprocess.check_output("svn status --xml".split(" "))
    for el in ElementTree.fromstring(svn_status).findall("./target/entry"):
        if  el.find('wc-status').get("item") not in ["added", "modified"]:
            continue
        path = el.get("path")
        if not path.endswith('.java'):
            continue
        with open(path, 'r') as f:
            filedata = f.read()
        filedata2 = re.sub("since xxx", lambda _: "since {}".format(get_revision()), filedata)
        if filedata != filedata2:
            print("replacing 'since xxx' with 'since {}' in '{}'".format(get_revision(), path))
            with open(path, 'w') as f:
                f.write(filedata2)

def get_revision():
    global revision
    if revision is not None:
        return revision
    svn_info_local = subprocess.check_output("svn info --xml".split(" "))
    rep_url = ElementTree.fromstring(svn_info_local).findtext("./entry/repository/root")
    svn_info_server = subprocess.check_output("svn info --xml".split(" ") + [rep_url])
    revision = int(ElementTree.fromstring(svn_info_server).find("./entry").get("revision")) + 1
    return revision
    
main()

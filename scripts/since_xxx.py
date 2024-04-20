#!/usr/bin/python3
# License: CC0

"""
Helper script to replace "@since xxx" in Javadoc by the upcoming revision
number.

Will retrieve the current revision number from the server. It runs over all
modified and added .java files and replaces xxx in "since xxx" by the revision
number that is to be expected for the next commit.
"""

import xml.etree.ElementTree as ElementTree
import subprocess
import re
import sys

revision = None


def main() -> None:
    """
    Do the main work of the script.
    :return: Nothing
    """
    args = sys.argv
    svn_status = subprocess.check_output("svn status --xml".split(" "))
    tree = ElementTree.fromstring(svn_status)
    rev = get_revision()
    if len(args) == 2:
        for change_set in tree.findall("./changelist"):
            if (
                "name" in change_set.attrib
                and change_set.attrib["name"] == args[1]
            ):
                for el in change_set.findall("./entry"):
                    write_xxx(rev, el)
    elif len(args) > 2:
        raise ValueError(
            "Too many args: only one changelist should be passed at a time, "
            "or none at all "
        )
    else:
        for el in tree.findall("./target/entry"):
            write_xxx(rev, el)


def write_xxx(rev: int, el: ElementTree.Element) -> None:
    """
    Write a revision to a changed file
    :param rev: The revision to write
    :param el: The element containing the path to the file to update
    :return: Nothing
    """
    if el.find("wc-status").get("item") not in ["added", "modified"]:
        return
    path = el.get("path")
    if not path.endswith(".java"):
        return
    with open(path, "r") as f:
        old_text = f.read()
    new_text = re.sub("since xxx", lambda _: "since {}".format(rev), old_text)
    if old_text != new_text:
        print(
            "replacing 'since xxx' with 'since {}' in '{}'".format(rev, path)
        )
        with open(path, "w") as f:
            f.write(new_text)


def get_revision() -> int:
    """
    Get the next revision
    :return: The current revision + 1
    """
    global revision
    if revision is not None:
        return revision
    svn_info_local = subprocess.check_output("svn info --xml".split(" "))
    rep_url = ElementTree.fromstring(svn_info_local).findtext(
        "./entry/repository/root"
    )
    svn_info_server = subprocess.check_output(
        "svn info --xml".split(" ") + [rep_url]
    )
    revision = (
        int(
            ElementTree.fromstring(svn_info_server)
            .find("./entry")
            .get("revision")
        )
        + 1
    )
    return revision


if __name__ == "__main__":
    main()

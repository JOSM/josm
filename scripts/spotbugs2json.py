#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ET
import enum
import json
import typing


class Severity(enum.Enum):
    INFO = list(range(15, 21))
    MINOR = list(range(10, 15))
    MAJOR = list(range(5, 10))
    CRITICAL = list(range(3, 5))
    BLOCKER = list(range(1, 3))

    def __str__(self) -> str:
        return self.name.lower()


class Categories(enum.Enum):
    BUG_RISK = ["BAD_PRACTICE", "CORRECTNESS"]
    CLARITY = ["NOISE"]
    COMPATIBILITY = ["I18N"]
    COMPLEXITY = []
    DUPLICATION = []
    PERFORMANCE = ["MT_CORRECTNESS", "PERFORMANCE"]
    SECURITY = ["EXPERIMENTAL", "MALICIOUS_CODE", "SECURITY"]
    STYLE = ["STYLE"]

    def __str__(self) -> str:
        return self.name.replace("_", " ").title()

class Project:
    source_directory: str = None


class Content:
    body: str

    def to_json(self) -> dict:
        return self.__dict__


class Location:
    path: str
    lines: typing.Dict[str, int]
    positions: typing.Dict[str, typing.Dict[str, int]]

    def to_json(self) -> dict:
        return self.__dict__


class Trace:
    locations: typing.List[Location]
    stacktrace: bool = False

    def to_json(self) -> dict:
        return self.__dict__


class Issue:
    issue_type: str = "issue"
    check_name: str
    description: str
    content: Content = None
    categories: typing.List[Categories]
    location: Location
    trace: Trace
    other_locations: typing.List[Location]
    remediation_points: int
    severity: Severity

    def to_json(self) -> dict:
        returnDict = {
            "type": self.issue_type,
            "check_name": self.check_name,
            "description": self.description,
            "categories": self.categories,
            "location": self.location.to_json(),
            "fingerprint": hash(self),
        }

        if hasattr(self, "content") and self.content:
            returnDict["content"] = self.content.to_json()
        if hasattr(self, "trace") and self.trace:
            returnDict["trace"] = self.trace.to_json()
        if hasattr(self, "remediation_points") and self.remediation_points:
            returnDict["remediation_points"] = self.remediation_points
        if hasattr(self, "severity") and self.severity:
            returnDict["severity"] = str(self.severity)

        if hasattr(self, "other_locations") and self.other_locations:
            returnDict["other_locations"] = [l.to_json() for l in self.other_locations]

        return returnDict

    def __hash__(self):
        i: int = 1
        h: int = 0
        for index in [
            "issue_type",
            "check_name",
            "description",
            "categories",
            "location",
            "severity",
            "content",
            "issue_type",
            "other_locations",
            "remediation_points",
            "severity",
            "trace",
        ]:
            if hasattr(self, index) and getattr(self, index):
                temp = getattr(self, index)
                try:
                    if isinstance(temp, list):
                        for j in temp:
                            h += i * hash(j)
                    else:
                        h += i * hash(temp)
                except TypeError as e:
                    print(e)
                    pass
            i += 31
        return h


def parseProject(element: ET.Element) -> Project:
    project = Project()
    for child in element:
        if child.tag == "SrcDir":
            project.source_directory = child.text
    return project


def parse_bug_instance(bug_instance: ET.Element) -> Issue:
    issue = Issue()
    issue.check_name = bug_instance.attrib["abbrev"]
    issue.description = bug_instance.attrib["type"]
    cat = bug_instance.attrib["category"]
    for category in Categories:
        if cat in category.value:
            if not hasattr(issue, "categories"):
                issue.categories = []
            issue.categories.append(cat)
    sev = int(bug_instance.attrib["rank"])
    for severity in Severity:
        if sev in severity.value:
            issue.severity = severity
            break

    for child in bug_instance:
        for child_2 in child:
            if "sourcepath" in child_2.attrib and "start" in child_2.attrib and "end" in child_2.attrib:
                location = Location()
                location.path = child_2.attrib["sourcepath"]
                location.lines = {}
                location.lines["begin"] = child_2.attrib["start"]
                location.lines["end"] = child_2.attrib["end"]
                if not hasattr(issue, "location"):
                    issue.location  = location
                elif not hasattr(issue, "other_locations"):
                    issue.other_locations = [location]
                else:
                    issue.other_locations.append(location)
    return issue


def main():
    tree = ET.parse("spotbugs-josm.xml")

    root = tree.getroot()

    issues = []
    project: Project = None

    for child in root:
        if child.tag == "Project":
            project = parseProject(child)
        elif child.tag == "BugInstance":
            issues.append(parse_bug_instance(child))

    with open("spotbugs-josm.json", 'w') as spotbugs_file:
        json.dump(issues, spotbugs_file, default=lambda o: o.to_json())


if __name__ == "__main__":
    main()

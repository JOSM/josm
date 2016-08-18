#!/usr/bin/perl
###########################################################################
# Java API Compliance Checker (JAPICC) 1.8
# A tool for checking backward compatibility of a Java library API
#
# Written by Andrey Ponomarenko
#
# Copyright (C) 2011 Institute for System Programming, RAS
# Copyright (C) 2011-2016 Andrey Ponomarenko's ABI Laboratory
#
# PLATFORMS
# =========
#  Linux, FreeBSD, Mac OS X, MS Windows
#
# REQUIREMENTS
# ============
#  Linux, FreeBSD, Mac OS X
#    - JDK or OpenJDK - development files (javap, javac)
#    - Perl 5 (5.8 or newer)
#
#  MS Windows
#    - JDK or OpenJDK (javap, javac)
#    - Active Perl 5 (5.8 or newer)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License or the GNU Lesser
# General Public License as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# and the GNU Lesser General Public License along with this program.
# If not, see <http://www.gnu.org/licenses/>.
###########################################################################
use Getopt::Long;
Getopt::Long::Configure ("posix_default", "no_ignore_case", "permute");
use File::Path qw(mkpath rmtree);
use File::Temp qw(tempdir);
use File::Copy qw(copy);
use File::Spec::Functions qw(abs2rel);
use Cwd qw(abs_path cwd);
use Data::Dumper;
use Digest::MD5 qw(md5_hex);
use Config;

my $TOOL_VERSION = "1.8";
my $API_DUMP_VERSION = "2.0";
my $API_DUMP_MAJOR = majorVersion($API_DUMP_VERSION);

my ($Help, $ShowVersion, %Descriptor, $TargetLibraryName, $CheckSeparately,
$TestSystem, $DumpAPI, $ClassListPath, $ClientPath, $StrictCompat,
$DumpVersion, $BinaryOnly, $TargetTitle, %TargetVersion, $SourceOnly,
$ShortMode, $KeepInternal, $OutputReportPath, $BinaryReportPath,
$SourceReportPath, $Debug, $Quick, $SortDump, $SkipDeprecated, $SkipClassesList,
$ShowAccess, $AffectLimit, $JdkPath, $SkipInternalPackages, $HideTemplates,
$HidePackages, $ShowPackages, $Minimal, $AnnotationsListPath,
$SkipPackagesList, $OutputDumpPath, $AllAffected, $Compact,
$SkipAnnotationsListPath, $ExternCss, $ExternJs, $SkipInternalTypes,
$AddedAnnotations, $RemovedAnnotations, $CountMethods, %DepDump, $OldStyle);

my $CmdName = get_filename($0);
my $OSgroup = get_OSgroup();
my $ORIG_DIR = cwd();
my $TMP_DIR = tempdir(CLEANUP=>1);
my $ARG_MAX = get_ARG_MAX();
my $REPRODUCIBLE = 1;
my $MD5_LEN = 8;

my %OS_Archive = (
    "windows"=>"zip",
    "default"=>"tar.gz"
);

my %ERROR_CODE = (
    # Compatible verdict
    "Compatible"=>0,
    "Success"=>0,
    # Incompatible verdict
    "Incompatible"=>1,
    # Undifferentiated error code
    "Error"=>2,
    # System command is not found
    "Not_Found"=>3,
    # Cannot access input files
    "Access_Error"=>4,
    # Invalid input API dump
    "Invalid_Dump"=>7,
    # Incompatible version of API dump
    "Dump_Version"=>8,
    # Cannot find a module
    "Module_Error"=>9
);

my %HomePage = (
    "Dev"=>"https://github.com/lvc/japi-compliance-checker",
    "Wiki"=>"http://ispras.linuxbase.org/index.php/Java_API_Compliance_Checker"
);

my $ShortUsage = "Java API Compliance Checker (JAPICC) $TOOL_VERSION
A tool for checking backward compatibility of a Java library API
Copyright (C) 2016 Andrey Ponomarenko's ABI Laboratory
License: GNU LGPL or GNU GPL

Usage: $CmdName [options]
Example: $CmdName OLD.jar NEW.jar

More info: $CmdName --help";

if($#ARGV==-1)
{
    printMsg("INFO", $ShortUsage);
    exit(0);
}

GetOptions("h|help!" => \$Help,
  "v|version!" => \$ShowVersion,
  "dumpversion!" => \$DumpVersion,
# general options
  "l|lib|library=s" => \$TargetLibraryName,
  "d1|old|o=s" => \$Descriptor{1}{"Path"},
  "d2|new|n=s" => \$Descriptor{2}{"Path"},
# extra options
  "client|app=s" => \$ClientPath,
  "binary|bin!" => \$BinaryOnly,
  "source|src!" => \$SourceOnly,
  "v1|version1|vnum=s" => \$TargetVersion{1},
  "v2|version2=s" => \$TargetVersion{2},
  "s|strict!" => \$StrictCompat,
  "keep-internal!" => \$KeepInternal,
  "skip-internal-packages|skip-internal=s" => \$SkipInternalPackages,
  "skip-internal-types=s" => \$SkipInternalTypes,
  "dump|dump-api=s" => \$DumpAPI,
  "classes-list=s" => \$ClassListPath,
  "annotations-list=s" => \$AnnotationsListPath,
  "skip-annotations-list=s" => \$SkipAnnotationsListPath,
  "skip-deprecated!" => \$SkipDeprecated,
  "skip-classes=s" => \$SkipClassesList,
  "skip-packages=s" => \$SkipPackagesList,
  "short" => \$ShortMode,
  "dump-path=s" => \$OutputDumpPath,
  "report-path=s" => \$OutputReportPath,
  "bin-report-path=s" => \$BinaryReportPath,
  "src-report-path=s" => \$SourceReportPath,
  "quick!" => \$Quick,
  "sort!" => \$SortDump,
  "show-access!" => \$ShowAccess,
  "limit-affected=s" => \$AffectLimit,
  "hide-templates!" => \$HideTemplates,
  "show-packages!" => \$ShowPackages,
  "compact!" => \$Compact,
  "added-annotations!" => \$AddedAnnotations,
  "removed-annotations!" => \$RemovedAnnotations,
  "count-methods=s" => \$CountMethods,
  "dep1=s" => \$DepDump{1},
  "dep2=s" => \$DepDump{2},
  "old-style!" => \$OldStyle,
# other options
  "test!" => \$TestSystem,
  "debug!" => \$Debug,
  "title=s" => \$TargetTitle,
  "jdk-path=s" => \$JdkPath,
  "external-css=s" => \$ExternCss,
  "external-js=s" => \$ExternJs,
# deprecated
  "minimal!" => \$Minimal,
  "hide-packages!" => \$HidePackages,
# private
  "all-affected!" => \$AllAffected
) or ERR_MESSAGE();

if(@ARGV)
{ 
    if($#ARGV==1)
    { # japi-compliance-checker OLD.jar NEW.jar
        $Descriptor{1}{"Path"} = $ARGV[0];
        $Descriptor{2}{"Path"} = $ARGV[1];
    }
    else {
        ERR_MESSAGE();
    }
}

sub ERR_MESSAGE()
{
    printMsg("INFO", "\n".$ShortUsage);
    exit($ERROR_CODE{"Error"});
}

my $AR_EXT = getAR_EXT($OSgroup);

my $HelpMessage="
NAME:
  Java API Compliance Checker ($CmdName)
  Check backward compatibility of a Java library API

DESCRIPTION:
  Java API Compliance Checker (JAPICC) is a tool for checking backward
  binary/source compatibility of a Java library API. The tool checks classes
  declarations of old and new versions and analyzes changes that may break
  compatibility: removed class members, added abstract methods, etc. Breakage
  of the binary compatibility may result in crashing or incorrect behavior of
  existing clients built with an old version of a library if they run with a
  new one. Breakage of the source compatibility may result in recompilation
  errors with a new library version.

  JAPICC is intended for library developers and operating system maintainers
  who are interested in ensuring backward compatibility (i.e. allow old clients
  to run or to be recompiled with a new version of a library).

  This tool is free software: you can redistribute it and/or modify it
  under the terms of the GNU LGPL or GNU GPL.

USAGE:
  $CmdName [options]

EXAMPLE:
  $CmdName OLD.jar NEW.jar
    OR
  $CmdName -lib NAME -old OLD.xml -new NEW.xml
  OLD.xml and NEW.xml are XML-descriptors:

    <version>
        1.0
    </version>
    
    <archives>
        /path1/to/JAR(s)/
        /path2/to/JAR(s)/
        ...
    </archives>

INFORMATION OPTIONS:
  -h|-help
      Print this help.

  -v|-version
      Print version information.

  -dumpversion
      Print the tool version ($TOOL_VERSION) and don't do anything else.

GENERAL OPTIONS:
  -l|-lib|-library NAME
      Library name (without version).

  -d1|-old|-o PATH
      Descriptor of 1st (old) library version.
      It may be one of the following:
      
         1. Java ARchive (*.jar)
         2. XML-descriptor (VERSION.xml file):

              <version>
                  1.0
              </version>
              
              <archives>
                  /path1/to/JAR(s)/
                  /path2/to/JAR(s)/
                   ...
              </archives>

                 ...
         
         3. API dump generated by -dump option

      If you are using 1, 4-6 descriptor types then you should
      specify version numbers with -v1 and -v2 options too.

      If you are using *.jar as a descriptor then the tool will try to
      get implementation version from MANIFEST.MF file.

  -d2|-new|-n PATH
      Descriptor of 2nd (new) library version.

EXTRA OPTIONS:
  -client|-app PATH
      This option allows to specify the client Java ARchive that should be
      checked for portability to the new library version.
      
  -binary|-bin
      Show \"Binary\" compatibility problems only.
      Generate report to \"bin_compat_report.html\".
      
  -source|-src
      Show \"Source\" compatibility problems only.
      Generate report to \"src_compat_report.html\".
      
  -v1|-version1 NUM
      Specify 1st API version outside the descriptor. This option is needed
      if you have prefered an alternative descriptor type (see -d1 option).
      
      In general case you should specify it in the XML descriptor:
          <version>
              VERSION
          </version>

  -v2|-version2 NUM
      Specify 2nd library version outside the descriptor.

  -vnum NUM
      Specify the library version in the generated API dump.

  -s|-strict
      Treat all API compatibility warnings as problems.

  -keep-internal
      Do not skip checking of these packages:
        *impl*
        *internal*
        *examples*
  
  -skip-internal-packages PATTERN
      Do not check packages matched by the pattern.
  
  -skip-internal-types PATTERN
      Do not check types (classes and interfaces) matched by the pattern.
  
  -dump|-dump-api PATH
      Dump library API to gzipped TXT format file. You can transfer it
      anywhere and pass instead of the descriptor. Also it may be used
      for debugging the tool. Compatible dump versions: $API_DUMP_MAJOR.0<=V<=$API_DUMP_VERSION
      
  -classes-list PATH
      This option allows to specify a file with a list
      of classes that should be checked, other classes will not be checked.
  
  -annotations-list PATH
      Specifies a file with a list of annotations. The tool will check only
      classes annotated by the annotations from the list. Other classes
      will not be checked.
  
  -skip-annotations-list PATH
      Skip checking of classes annotated by the annotations in the list.
      
  -skip-deprecated
      Skip analysis of deprecated methods and classes.
      
  -skip-classes PATH
      This option allows to specify a file with a list
      of classes that should not be checked.
      
  -skip-packages PATH
      This option allows to specify a file with a list
      of packages that should not be checked.
      
  -short
      Do not list added/removed methods.
  
  -dump-path PATH
      Specify a *.api.$AR_EXT or *.api file path where to generate an API dump.
      Default: 
          abi_dumps/LIB_NAME/LIB_NAME_VERSION.api.$AR_EXT

  -report-path PATH
      Path to compatibility report.
      Default: 
          compat_reports/LIB_NAME/V1_to_V2/compat_report.html

  -bin-report-path PATH
      Path to \"Binary\" compatibility report.
      Default: 
          compat_reports/LIB_NAME/V1_to_V2/bin_compat_report.html

  -src-report-path PATH
      Path to \"Source\" compatibility report.
      Default: 
          compat_reports/LIB_NAME/V1_to_V2/src_compat_report.html

  -quick
      Quick analysis.
      Disabled:
        - analysis of method parameter names
        - analysis of class field values
        - analysis of usage of added abstract methods
        - distinction of deprecated methods and classes

  -sort
      Enable sorting of data in API dumps.
      
  -show-access
      Show access level of non-public methods listed in the report.
      
  -hide-templates
      Hide template parameters in the report.
  
  -hide-packages
  -minimal
      Do nothing.
  
  -show-packages
      Show package names in the report.
      
  -limit-affected LIMIT
      The maximum number of affected methods listed under the description
      of the changed type in the report.
  
  -compact
      Try to simplify formatting and reduce size of the report (for a big
      set of changes).
  
  -added-annotations
      Apply filters by annotations only to new version of the library.
  
  -removed-annotations
      Apply filters by annotations only to old version of the library.
  
  -count-methods PATH
      Count total public methods in the API dump.
  
  -dep1 PATH
  -dep2 PATH
      Path to the API dump of the required dependency archive. It will
      be used to resolve overwritten methods and more.
  
  -old-style
      Generate old-style report.

OTHER OPTIONS:
  -test
      Run internal tests. Create two incompatible versions of a sample library
      and run the tool to check them for compatibility. This option allows to
      check if the tool works correctly in the current environment.

  -debug
      Debugging mode. Print debug info on the screen. Save intermediate
      analysis stages in the debug directory:
          debug/LIB_NAME/VER/

      Also consider using -dump option for debugging the tool.

  -title NAME
      Change library name in the report title to NAME. By default
      will be displayed a name specified by -l option.

  -jdk-path PATH
      Path to the JDK install tree (e.g. /usr/lib/jvm/java-7-openjdk-amd64).

  -external-css PATH
      Generate CSS styles file to PATH. This helps to save space when
      generating thousands of reports.

  -external-js PATH
      Generate JS script file to PATH.

REPORT:
    Compatibility report will be generated to:
        compat_reports/LIB_NAME/V1_to_V2/compat_report.html

EXIT CODES:
    0 - Compatible. The tool has run without any errors.
    non-zero - Incompatible or the tool has run with errors.

MORE INFORMATION:
    ".$HomePage{"Wiki"}."
    ".$HomePage{"Dev"}."\n\n";

sub HELP_MESSAGE()
{ # -help
    printMsg("INFO", $HelpMessage."\n");
}

my %TypeProblems_Kind=(
    "Binary"=>{
        "NonAbstract_Class_Added_Abstract_Method"=>"High",
        "Abstract_Class_Added_Abstract_Method"=>"Safe",
        "Abstract_Class_Added_Abstract_Method_Invoked_By_Others"=>"Medium",
        "Class_Removed_Abstract_Method"=>"High",
        "Interface_Added_Abstract_Method"=>"Safe",
        "Interface_Added_Abstract_Method_Invoked_By_Others"=>"Medium",
        "Interface_Removed_Abstract_Method"=>"High",
        "Removed_Class"=>"High",
        "Removed_Interface"=>"High",
        "Class_Method_Became_Abstract"=>"High",
        "Class_Method_Became_NonAbstract"=>"Low",
        "Interface_Method_Became_NonDefault"=>"High",
        "Interface_Method_Became_Default"=>"Safe",
        "Added_Super_Class"=>"Low",
        "Abstract_Class_Added_Super_Abstract_Class"=>"Safe",
        "Abstract_Class_Added_Super_Abstract_Class_Invoked_By_Others"=>"Medium",
        "Removed_Super_Class"=>"Medium",
        "Changed_Super_Class"=>"Medium",
        "Abstract_Class_Added_Super_Interface"=>"Safe",
        "Abstract_Class_Added_Super_Interface_Invoked_By_Others"=>"Medium",
        "Abstract_Class_Added_Super_Interface_With_Implemented_Methods"=>"Safe",
        "Class_Removed_Super_Interface"=>"High",
        "Interface_Added_Super_Interface"=>"Safe",
        "Interface_Added_Super_Interface_Used_By_Others"=>"Medium",
        "Interface_Added_Super_Constant_Interface"=>"Low",
        "Interface_Added_Super_Interface_With_Implemented_Methods"=>"Safe",
        "Interface_Removed_Super_Interface"=>"High",
        "Interface_Removed_Super_Constant_Interface"=>"Safe",
        "Class_Became_Interface"=>"High",
        "Interface_Became_Class"=>"High",
        "Class_Became_Final"=>"High",
        "Class_Became_Abstract"=>"High",
        "Class_Added_Field"=>"Safe",
        "Interface_Added_Field"=>"Safe",
        "Removed_NonConstant_Field"=>"High",
        "Removed_Constant_Field"=>"Low",
        "Renamed_Field"=>"High",
        "Renamed_Constant_Field"=>"Low",
        "Changed_Field_Type"=>"High",
        "Changed_Field_Access"=>"High",
        "Changed_Final_Field_Value"=>"Medium",
        "Changed_Final_Version_Field_Value"=>"Low",
        "Field_Became_Final"=>"Medium",
        "Field_Became_NonFinal"=>"Low",
        "NonConstant_Field_Became_Static"=>"High",
        "NonConstant_Field_Became_NonStatic"=>"High",
        "Class_Overridden_Method"=>"Low",
        "Class_Method_Moved_Up_Hierarchy"=>"Low"
    },
    "Source"=>{
        "NonAbstract_Class_Added_Abstract_Method"=>"High",
        "Abstract_Class_Added_Abstract_Method"=>"High",
        "Abstract_Class_Added_Abstract_Method_Invoked_By_Others"=>"High",
        "Interface_Added_Abstract_Method"=>"High",
        "Interface_Added_Abstract_Method_Invoked_By_Others"=>"High",
        "Class_Removed_Abstract_Method"=>"High",
        "Interface_Removed_Abstract_Method"=>"High",
        "Removed_Class"=>"High",
        "Removed_Interface"=>"High",
        "Class_Method_Became_Abstract"=>"High",
        "Class_Method_Became_NonAbstract"=>"Safe",
        "Interface_Method_Became_NonDefault"=>"High",
        "Interface_Method_Became_Default"=>"Safe",
        "Added_Super_Class"=>"Low",
        "Abstract_Class_Added_Super_Abstract_Class"=>"High",
        "Abstract_Class_Added_Super_Abstract_Class_Invoked_By_Others"=>"High",
        "Removed_Super_Class"=>"Medium",
        "Changed_Super_Class"=>"Medium",
        "Abstract_Class_Added_Super_Interface"=>"High",
        "Abstract_Class_Added_Super_Interface_Invoked_By_Others"=>"High",
        "Abstract_Class_Added_Super_Interface_With_Implemented_Methods"=>"Safe",
        "Class_Removed_Super_Interface"=>"High",
        "Interface_Added_Super_Interface"=>"High",
        "Interface_Added_Super_Interface_Used_By_Others"=>"High",
        "Interface_Added_Super_Constant_Interface"=>"Low",
        "Interface_Added_Super_Interface_With_Implemented_Methods"=>"Safe",
        "Interface_Removed_Super_Interface"=>"High",
        "Interface_Removed_Super_Constant_Interface"=>"High",
        "Class_Became_Interface"=>"High",
        "Interface_Became_Class"=>"High",
        "Class_Became_Final"=>"High",
        "Class_Became_Abstract"=>"High",
        "Class_Added_Field"=>"Safe",
        "Interface_Added_Field"=>"Safe",
        "Removed_NonConstant_Field"=>"High",
        "Removed_Constant_Field"=>"High",
        "Renamed_Field"=>"High",
        "Renamed_Constant_Field"=>"High",
        "Changed_Field_Type"=>"High",
        "Changed_Field_Access"=>"High",
        "Field_Became_Final"=>"Medium",
        "Constant_Field_Became_NonStatic"=>"High",
        "NonConstant_Field_Became_NonStatic"=>"High",
        "Removed_Annotation"=>"High"
    }
);

my %MethodProblems_Kind=(
    "Binary"=>{
        "Added_Method"=>"Safe",
        "Removed_Method"=>"High",
        "Method_Became_Static"=>"High",
        "Method_Became_NonStatic"=>"High",
        "NonStatic_Method_Became_Final"=>"Medium",
        "Changed_Method_Access"=>"High",
        "Method_Became_Synchronized"=>"Low",
        "Method_Became_NonSynchronized"=>"Low",
        "Method_Became_Abstract"=>"High",
        "Method_Became_NonAbstract"=>"Low",
        "Method_Became_NonDefault"=>"High",
        "Method_Became_Default"=>"Safe",
        "NonAbstract_Method_Added_Checked_Exception"=>"Low",
        "NonAbstract_Method_Removed_Checked_Exception"=>"Low",
        "Added_Unchecked_Exception"=>"Low",
        "Removed_Unchecked_Exception"=>"Low",
        "Variable_Arity_To_Array"=>"Low",# not implemented yet
        "Changed_Method_Return_From_Void"=>"High"
    },
    "Source"=>{
        "Added_Method"=>"Safe",
        "Removed_Method"=>"High",
        "Method_Became_Static"=>"Low",
        "Method_Became_NonStatic"=>"High",
        "Static_Method_Became_Final"=>"Medium",
        "NonStatic_Method_Became_Final"=>"Medium",
        "Changed_Method_Access"=>"High",
        "Method_Became_Abstract"=>"High",
        "Method_Became_NonAbstract"=>"Safe",
        "Method_Became_NonDefault"=>"High",
        "Method_Became_Default"=>"Safe",
        "Abstract_Method_Added_Checked_Exception"=>"Medium",
        "NonAbstract_Method_Added_Checked_Exception"=>"Medium",
        "Abstract_Method_Removed_Checked_Exception"=>"Medium",
        "NonAbstract_Method_Removed_Checked_Exception"=>"Medium"
    }
);

my %KnownRuntimeExceptions= map {$_=>1} (
# To separate checked- and unchecked- exceptions
    "java.lang.AnnotationTypeMismatchException",
    "java.lang.ArithmeticException",
    "java.lang.ArrayStoreException",
    "java.lang.BufferOverflowException",
    "java.lang.BufferUnderflowException",
    "java.lang.CannotRedoException",
    "java.lang.CannotUndoException",
    "java.lang.ClassCastException",
    "java.lang.CMMException",
    "java.lang.ConcurrentModificationException",
    "java.lang.DataBindingException",
    "java.lang.DOMException",
    "java.lang.EmptyStackException",
    "java.lang.EnumConstantNotPresentException",
    "java.lang.EventException",
    "java.lang.IllegalArgumentException",
    "java.lang.IllegalMonitorStateException",
    "java.lang.IllegalPathStateException",
    "java.lang.IllegalStateException",
    "java.lang.ImagingOpException",
    "java.lang.IncompleteAnnotationException",
    "java.lang.IndexOutOfBoundsException",
    "java.lang.JMRuntimeException",
    "java.lang.LSException",
    "java.lang.MalformedParameterizedTypeException",
    "java.lang.MirroredTypeException",
    "java.lang.MirroredTypesException",
    "java.lang.MissingResourceException",
    "java.lang.NegativeArraySizeException",
    "java.lang.NoSuchElementException",
    "java.lang.NoSuchMechanismException",
    "java.lang.NullPointerException",
    "java.lang.ProfileDataException",
    "java.lang.ProviderException",
    "java.lang.RasterFormatException",
    "java.lang.RejectedExecutionException",
    "java.lang.SecurityException",
    "java.lang.SystemException",
    "java.lang.TypeConstraintException",
    "java.lang.TypeNotPresentException",
    "java.lang.UndeclaredThrowableException",
    "java.lang.UnknownAnnotationValueException",
    "java.lang.UnknownElementException",
    "java.lang.UnknownEntityException",
    "java.lang.UnknownTypeException",
    "java.lang.UnmodifiableSetException",
    "java.lang.UnsupportedOperationException",
    "java.lang.WebServiceException",
    "java.lang.WrongMethodTypeException"
);

my %Slash_Type=(
    "default"=>"/",
    "windows"=>"\\"
);

my $SLASH = $Slash_Type{$OSgroup}?$Slash_Type{$OSgroup}:$Slash_Type{"default"};

my %OS_AddPath=(
# this data needed if tool can't detect it automatically
"macos"=>{
    "bin"=>{"/Developer/usr/bin"=>1}},
"beos"=>{
    "bin"=>{"/boot/common/bin"=>1,"/boot/system/bin"=>1,"/boot/develop/abi"=>1}}
);

#Global variables
my %RESULT;
my $ExtractCounter = 0;
my %Cache;
my $TOP_REF = "<a class='top_ref' href='#Top'>to the top</a>";
my %DEBUG_PATH;

#Types
my %TypeInfo;
my $TYPE_ID = 0;
my %CheckedTypes;
my %TName_Tid;
my %Class_Constructed;

#Classes
my %ClassList_User;
my %UsedMethods_Client;
my %UsedFields_Client;
my %UsedClasses_Client;
my %LibArchives;
my %Class_Methods;
my %Class_AbstractMethods;
my %Class_Fields;
my %MethodUsed;
my %ClassMethod_AddedUsed;
# my %FieldUsed;

#java.lang.Object
my %JavaObjectMethod = (
    
    "java/lang/Object.clone:()Ljava/lang/Object;" => 1,
    "java/lang/Object.equals:(Ljava/lang/Object;)Z" => 1,
    "java/lang/Object.finalize:()V" => 1,
    "java/lang/Object.getClass:()Ljava/lang/Class;" => 1,
    "java/lang/Object.hashCode:()I" => 1,
    "java/lang/Object.notify:()V" => 1,
    "java/lang/Object.notifyAll:()V" => 1,
    "java/lang/Object.toString:()Ljava/lang/String;" => 1,
    "java/lang/Object.wait:()V" => 1,
    "java/lang/Object.wait:(J)V" => 1,
    "java/lang/Object.wait:(JI)V" => 1
);

#Annotations
my %AnnotationList_User;
my %SkipAnnotationList_User;

#Methods
my %CheckedMethods;
my %tr_name;

#Merging
my %MethodInfo;
my $Version;
my %AddedMethod_Abstract;
my %RemovedMethod_Abstract;
my %ChangedReturnFromVoid;
my %SkipClasses;
my %SkipPackages;
my %KeepPackages;
my %SkippedPackage;

#Report
my %TypeChanges;

#Recursion locks
my @RecurSymlink;
my @RecurTypes;

#System
my %SystemPaths;
my %DefaultBinPaths;

#Problem descriptions
my %CompatProblems;
my %TotalAffected;

#Speedup
my %TypeProblemsIndex;

#Rerort
my $ContentID = 1;
my $ContentSpanStart = "<span class=\"section\" onclick=\"sC(this, 'CONTENT_ID')\">\n";
my $ContentSpanStart_Affected = "<span class=\"sect_aff\" onclick=\"sC(this, 'CONTENT_ID')\">\n";
my $ContentSpanEnd = "</span>\n";
my $ContentDivStart = "<div id=\"CONTENT_ID\" style=\"display:none;\">\n";
my $ContentDivEnd = "</div>\n";
my $Content_Counter = 0;

#Modes
my $JoinReport = 1;
my $DoubleReport = 0;

sub get_CmdPath($)
{
    my $Name = $_[0];
    return "" if(not $Name);
    if(defined $Cache{"get_CmdPath"}{$Name}) {
        return $Cache{"get_CmdPath"}{$Name};
    }
    my $Path = search_Cmd($Name);
    if(not $Path and $OSgroup eq "windows")
    { # search for *.exe file
        $Path=search_Cmd($Name.".exe");
    }
    if (not $Path) {
        $Path=search_Cmd_Path($Name);
    }
    if($Path=~/\s/) {
        $Path = "\"".$Path."\"";
    }
    return ($Cache{"get_CmdPath"}{$Name} = $Path);
}

sub search_Cmd($)
{
    my $Name = $_[0];
    return "" if(not $Name);
    if(defined $Cache{"search_Cmd"}{$Name}) {
        return $Cache{"search_Cmd"}{$Name};
    }
    if(defined $JdkPath)
    {
        if(-x $JdkPath."/".$Name) {
            return ($Cache{"search_Cmd"}{$Name} = $JdkPath."/".$Name);
        }
        
        if(-x $JdkPath."/bin/".$Name) {
            return ($Cache{"search_Cmd"}{$Name} = $JdkPath."/bin/".$Name);
        }
    }
    if(my $DefaultPath = get_CmdPath_Default($Name)) {
        return ($Cache{"search_Cmd"}{$Name} = $DefaultPath);
    }
    return ($Cache{"search_Cmd"}{$Name} = "");
}

sub search_Cmd_Path($)
{
    my $Name = $_[0];
    return "" if(not $Name);
    
    if(defined $Cache{"search_Cmd_Path"}{$Name}) {
        return $Cache{"search_Cmd_Path"}{$Name};
    }
    
    if(defined $SystemPaths{"bin"})
    {
        foreach my $Path (sort {length($a)<=>length($b)} keys(%{$SystemPaths{"bin"}}))
        {
            if(-f $Path."/".$Name or -f $Path."/".$Name.".exe") {
                return ($Cache{"search_Cmd_Path"}{$Name} = joinPath($Path,$Name));
            }
        }
    }

    return ($Cache{"search_Cmd_Path"}{$Name} = "");
}

sub get_CmdPath_Default($)
{ # search in PATH
    return "" if(not $_[0]);
    if(defined $Cache{"get_CmdPath_Default"}{$_[0]}) {
        return $Cache{"get_CmdPath_Default"}{$_[0]};
    }
    return ($Cache{"get_CmdPath_Default"}{$_[0]} = get_CmdPath_Default_I($_[0]));
}

sub get_CmdPath_Default_I($)
{ # search in PATH
    my $Name = $_[0];
    if($Name=~/find/)
    { # special case: search for "find" utility
        if(`find \"$TMP_DIR\" -maxdepth 0 2>\"$TMP_DIR/null\"`) {
            return "find";
        }
    }
    if(get_version($Name)) {
        return $Name;
    }
    if($OSgroup eq "windows")
    {
        if(`$Name /? 2>\"$TMP_DIR/null\"`) {
            return $Name;
        }
    }
    if($Name!~/which/)
    {
        if(my $WhichCmd = get_CmdPath("which"))
        {
            if(`$WhichCmd $Name 2>\"$TMP_DIR/null\"`) {
                return $Name;
            }
        }
    }
    foreach my $Path (sort {length($a)<=>length($b)} keys(%DefaultBinPaths))
    {
        if(-f $Path."/".$Name) {
            return joinPath($Path,$Name);
        }
    }
    return "";
}

sub showPos($)
{
    my $Number = $_[0];
    if(not $Number) {
        $Number = 1;
    }
    else {
        $Number = int($Number)+1;
    }
    if($Number>3) {
        return $Number."th";
    }
    elsif($Number==1) {
        return "1st";
    }
    elsif($Number==2) {
        return "2nd";
    }
    elsif($Number==3) {
        return "3rd";
    }
    else {
        return $Number;
    }
}

sub getAR_EXT($)
{
    my $Target = $_[0];
    if(my $Ext = $OS_Archive{$Target}) {
        return $Ext;
    }
    return $OS_Archive{"default"};
}

sub readDescriptor($$)
{
    my ($LibVersion, $Content) = @_;
    return if(not $LibVersion);
    my $DName = $DumpAPI?"descriptor":"descriptor \"d$LibVersion\"";
    if(not $Content) {
        exitStatus("Error", "$DName is empty");
    }
    if($Content!~/\</) {
        exitStatus("Error", "descriptor should be one of the following:\n  Java ARchive, XML descriptor, gzipped API dump or directory with Java ARchives.");
    }
    $Content=~s/\/\*(.|\n)+?\*\///g;
    $Content=~s/<\!--(.|\n)+?-->//g;
    $Descriptor{$LibVersion}{"Version"} = parseTag(\$Content, "version");
    $Descriptor{$LibVersion}{"Version"} = $TargetVersion{$LibVersion} if($TargetVersion{$LibVersion});
    if($Descriptor{$LibVersion}{"Version"} eq "") {
        exitStatus("Error", "version in the $DName is not specified (<version> section)");
    }
    
    my $DArchives = parseTag(\$Content, "archives");
    if(not $DArchives){
        exitStatus("Error", "Java ARchives in the $DName are not specified (<archive> section)");
    }
    else
    {# append the descriptor Java ARchives list
        if($Descriptor{$LibVersion}{"Archives"}) {
            $Descriptor{$LibVersion}{"Archives"} .= "\n".$DArchives;
        }
        else {
            $Descriptor{$LibVersion}{"Archives"} = $DArchives;
        }
        foreach my $Path (split(/\s*\n\s*/, $DArchives))
        {
            if(not -e $Path) {
                exitStatus("Access_Error", "can't access \'$Path\'");
            }
        }
    }
    foreach my $Package (split(/\s*\n\s*/, parseTag(\$Content, "skip_packages"))) {
        $SkipPackages{$LibVersion}{$Package} = 1;
    }
    foreach my $Package (split(/\s*\n\s*/, parseTag(\$Content, "packages"))) {
        $KeepPackages{$LibVersion}{$Package} = 1;
    }
}

sub parseTag($$)
{
    my ($CodeRef, $Tag) = @_;
    return "" if(not $CodeRef or not ${$CodeRef} or not $Tag);
    if(${$CodeRef}=~s/\<\Q$Tag\E\>((.|\n)+?)\<\/\Q$Tag\E\>//)
    {
        my $Content = $1;
        $Content=~s/(\A\s+|\s+\Z)//g;
        return $Content;
    }
    else {
        return "";
    }
}

sub ignore_path($$)
{
    my ($Path, $Prefix) = @_;
    return 1 if(not $Path or not -e $Path
    or not $Prefix or not -e $Prefix);
    return 1 if($Path=~/\~\Z/);# skipping system backup files
    # skipping hidden .svn, .git, .bzr, .hg and CVS directories
    return 1 if(cut_path_prefix($Path, $Prefix)=~/(\A|[\/\\]+)(\.(svn|git|bzr|hg)|CVS)([\/\\]+|\Z)/);
    return 0;
}

sub cut_path_prefix($$)
{
    my ($Path, $Prefix) = @_;
    $Prefix=~s/[\/\\]+\Z//;
    $Path=~s/\A\Q$Prefix\E([\/\\]+|\Z)//;
    return $Path;
}

sub get_filename($)
{ # much faster than basename() from File::Basename module
    if(defined $Cache{"get_filename"}{$_[0]}) {
        return $Cache{"get_filename"}{$_[0]};
    }
    if($_[0] and $_[0]=~/([^\/\\]+)[\/\\]*\Z/) {
        return ($Cache{"get_filename"}{$_[0]}=$1);
    }
    return ($Cache{"get_filename"}{$_[0]}="");
}

sub get_dirname($)
{ # much faster than dirname() from File::Basename module
    if(defined $Cache{"get_dirname"}{$_[0]}) {
        return $Cache{"get_dirname"}{$_[0]};
    }
    if($_[0] and $_[0]=~/\A(.*?)[\/\\]+[^\/\\]*[\/\\]*\Z/) {
        return ($Cache{"get_dirname"}{$_[0]}=$1);
    }
    return ($Cache{"get_dirname"}{$_[0]}="");
}

sub separate_path($) {
    return (get_dirname($_[0]), get_filename($_[0]));
}

sub joinPath($$)
{
    return join($SLASH, @_);
}

sub get_abs_path($)
{ # abs_path() should NOT be called for absolute inputs
  # because it can change them
    my $Path = $_[0];
    if(not is_abs($Path)) {
        $Path = abs_path($Path);
    }
    return $Path;
}

sub is_abs($) {
    return ($_[0]=~/\A(\/|\w+:[\/\\])/);
}

sub cmd_find($$$$)
{
    my ($Path, $Type, $Name, $MaxDepth) = @_;
    return () if(not $Path or not -e $Path);
    if($OSgroup eq "windows")
    {
        my $DirCmd = get_CmdPath("dir");
        if(not $DirCmd) {
            exitStatus("Not_Found", "can't find \"dir\" command");
        }
        $Path=~s/[\\]+\Z//;
        $Path = get_abs_path($Path);
        my $Cmd = $DirCmd." \"$Path\" /B /O";
        if($MaxDepth!=1) {
            $Cmd .= " /S";
        }
        if($Type eq "d") {
            $Cmd .= " /AD";
        }
        my @Files = ();
        if($Name)
        { # FIXME: how to search file names in MS shell?
            $Name=~s/\*/.*/g if($Name!~/\]/);
            foreach my $File (split(/\n/, `$Cmd`))
            {
                if($File=~/$Name\Z/i) {
                    push(@Files, $File);    
                }
            }
        }
        else {
            @Files = split(/\n/, `$Cmd 2>\"$TMP_DIR/null\"`);
        }
        my @AbsPaths = ();
        foreach my $File (@Files)
        {
            if(not is_abs($File)) {
                $File = joinPath($Path, $File);
            }
            if($Type eq "f" and not -f $File)
            { # skip dirs
                next;
            }
            push(@AbsPaths, $File);
        }
        if($Type eq "d") {
            push(@AbsPaths, $Path);
        }
        return @AbsPaths;
    }
    else
    {
        my $FindCmd = get_CmdPath("find");
        if(not $FindCmd) {
            exitStatus("Not_Found", "can't find a \"find\" command");
        }
        $Path = get_abs_path($Path);
        if(-d $Path and -l $Path
        and $Path!~/\/\Z/)
        { # for directories that are symlinks
            $Path.="/";
        }
        my $Cmd = $FindCmd." \"$Path\"";
        if($MaxDepth) {
            $Cmd .= " -maxdepth $MaxDepth";
        }
        if($Type) {
            $Cmd .= " -type $Type";
        }
        if($Name)
        {
            if($Name=~/\]/) {
                $Cmd .= " -regex \"$Name\"";
            }
            else {
                $Cmd .= " -name \"$Name\"";
            }
        }
        return split(/\n/, `$Cmd 2>\"$TMP_DIR/null\"`);
    }
}

sub path_format($$)
{ # forward slash to pass into MinGW GCC
    my ($Path, $Fmt) = @_;
    if($Fmt eq "windows")
    {
        $Path=~s/\//\\/g;
        $Path=lc($Path);
    }
    else {
        $Path=~s/\\/\//g;
    }
    return $Path;
}

sub unpackDump($)
{
    my $Path = $_[0];
    return "" if(not $Path or not -e $Path);
    
    if(isDumpFile($Path)) {
        return $Path;
    }
    
    $Path = get_abs_path($Path);
    $Path = path_format($Path, $OSgroup);
    my ($Dir, $FileName) = separate_path($Path);
    my $UnpackDir = $TMP_DIR."/unpack";
    if(-d $UnpackDir) {
        rmtree($UnpackDir);
    }
    mkpath($UnpackDir);
    if($FileName=~s/\Q.zip\E\Z//g)
    { # *.zip
        my $UnzipCmd = get_CmdPath("unzip");
        if(not $UnzipCmd) {
            exitStatus("Not_Found", "can't find \"unzip\" command");
        }
        chdir($UnpackDir);
        system("$UnzipCmd \"$Path\" >contents.txt");
        if($?) {
            exitStatus("Error", "can't extract \'$Path\'");
        }
        chdir($ORIG_DIR);
        my @Contents = ();
        foreach (split("\n", readFile("$UnpackDir/contents.txt")))
        {
            if(/inflating:\s*([^\s]+)/) {
                push(@Contents, $1);
            }
        }
        if(not @Contents) {
            exitStatus("Error", "can't extract \'$Path\'");
        }
        return joinPath($UnpackDir, $Contents[0]);
    }
    elsif($FileName=~s/\Q.tar.gz\E\Z//g)
    { # *.tar.gz
        if($OSgroup eq "windows")
        { # -xvzf option is not implemented in tar.exe (2003)
          # use "gzip.exe -k -d -f" + "tar.exe -xvf" instead
            my $TarCmd = get_CmdPath("tar");
            if(not $TarCmd) {
                exitStatus("Not_Found", "can't find \"tar\" command");
            }
            my $GzipCmd = get_CmdPath("gzip");
            if(not $GzipCmd) {
                exitStatus("Not_Found", "can't find \"gzip\" command");
            }
            chdir($UnpackDir);
            qx/$GzipCmd -k -d -f "$Path"/; # keep input files (-k)
            if($?) {
                exitStatus("Error", "can't extract \'$Path\'");
            }
            my @Contents = qx/$TarCmd -xvf "$Dir\\$FileName.tar"/;
            if($? or not @Contents) {
                exitStatus("Error", "can't extract \'$Path\'");
            }
            chdir($ORIG_DIR);
            unlink($Dir."/".$FileName.".tar");
            chomp $Contents[0];
            return joinPath($UnpackDir, $Contents[0]);
        }
        else
        { # Linux, Unix, OS X
            my $TarCmd = get_CmdPath("tar");
            if(not $TarCmd) {
                exitStatus("Not_Found", "can't find \"tar\" command");
            }
            chdir($UnpackDir);
            my @Contents = qx/$TarCmd -xvzf "$Path" 2>&1/;
            if($? or not @Contents) {
                exitStatus("Error", "can't extract \'$Path\'");
            }
            chdir($ORIG_DIR);
            $Contents[0]=~s/^x //; # OS X
            chomp $Contents[0];
            return joinPath($UnpackDir, $Contents[0]);
        }
    }
}

sub mergeClasses()
{
    my %ReportedRemoved = undef;
    
    foreach my $ClassName (keys(%{$Class_Methods{1}}))
    {
        next if(not $ClassName);
        my $Type1_Id = $TName_Tid{1}{$ClassName};
        my %Type1 = get_Type($Type1_Id, 1);
        
        if($Type1{"Type"}!~/class|interface/) {
            next;
        }
        
        if(defined $Type1{"Access"}
        and $Type1{"Access"}=~/private/) {
            next;
        }
        
        if(not classFilter(\%Type1, 1, 0)) {
            next;
        }
        
        my $Type2_Id = $TName_Tid{2}{$ClassName};
        if(not $Type2_Id)
        { # classes and interfaces with public methods
            foreach my $Method (keys(%{$Class_Methods{1}{$ClassName}}))
            {
                if(not methodFilter($Method, 1)) {
                    next;
                }
                
                $CheckedTypes{$ClassName} = 1;
                $CheckedMethods{$Method} = 1;
                
                if($Type1{"Type"} eq "class")
                {
                    %{$CompatProblems{$Method}{"Removed_Class"}{"this"}} = (
                        "Type_Name"=>$ClassName,
                        "Target"=>$ClassName  );
                }
                else
                {
                    %{$CompatProblems{$Method}{"Removed_Interface"}{"this"}} = (
                        "Type_Name"=>$ClassName,
                        "Target"=>$ClassName  );
                }
                
                $ReportedRemoved{$ClassName} = 1;
            }
        }
    }
    
    foreach my $Class_Id (keys(%{$TypeInfo{1}}))
    {
        my %Class1 = get_Type($Class_Id, 1);
        
        if($Class1{"Type"}!~/class|interface/) {
            next;
        }
        
        if(defined $Class1{"Access"}
        and $Class1{"Access"}=~/private/) {
            next;
        }
        
        if(not classFilter(\%Class1, 1, 1)) {
            next;
        }
        
        my $ClassName = $Class1{"Name"};
        
        if(my $Class2_Id = $TName_Tid{2}{$ClassName})
        { # classes and interfaces with public static fields
            if(not defined $Class_Methods{1}{$ClassName})
            {
                my %Class2 = get_Type($Class2_Id, 2);
                
                foreach my $Field (keys(%{$Class1{"Fields"}}))
                {
                    my $FieldInfo = $Class1{"Fields"}{$Field};
                    
                    my $FAccess = $FieldInfo->{"Access"};
                    if($FAccess=~/private/) {
                        next;
                    }
                    
                    if($FieldInfo->{"Static"})
                    {
                        $CheckedTypes{$ClassName} = 1;
                        
                        if(not defined $Class2{"Fields"}{$Field})
                        {
                            %{$CompatProblems{".client_method"}{"Removed_NonConstant_Field"}{$Field}}=(
                                "Target"=>$Field,
                                "Type_Name"=>$ClassName,
                                "Type_Type"=>$Class1{"Type"},
                                "Field_Type"=>get_TypeName($FieldInfo->{"Type"}, 1)  );
                        }
                    }
                }
            }
        }
        else
        { # removed
            if(defined $Class1{"Annotation"})
            {
                %{$CompatProblems{".client_method"}{"Removed_Annotation"}{"this"}} = (
                    "Type_Name"=>$ClassName,
                    "Target"=>$ClassName  );
            }
            
            if(not defined $Class_Methods{1}{$ClassName})
            {
                # classes and interfaces with public static fields
                if(not defined $ReportedRemoved{$ClassName})
                {
                    foreach my $Field (keys(%{$Class1{"Fields"}}))
                    {
                        my $FieldInfo = $Class1{"Fields"}{$Field};
                        
                        my $FAccess = $FieldInfo->{"Access"};
                        if($FAccess=~/private/) {
                            next;
                        }
                        
                        if($FieldInfo->{"Static"})
                        {
                            $CheckedTypes{$ClassName} = 1;
                            
                            if($Class1{"Type"} eq "class")
                            {
                                %{$CompatProblems{".client_method"}{"Removed_Class"}{"this"}} = (
                                    "Type_Name"=>$ClassName,
                                    "Target"=>$ClassName  );
                            }
                            else
                            {
                                %{$CompatProblems{".client_method"}{"Removed_Interface"}{"this"}} = (
                                    "Type_Name"=>$ClassName,
                                    "Target"=>$ClassName  );
                            }
                        }
                    }
                }
            }
        }
    }
}

sub findFieldPair($$)
{
    my ($Field_Pos, $Pair_Type) = @_;
    foreach my $Pair_Name (sort keys(%{$Pair_Type->{"Fields"}}))
    {
        if(defined $Pair_Type->{"Fields"}{$Pair_Name})
        {
            if($Pair_Type->{"Fields"}{$Pair_Name}{"Pos"} eq $Field_Pos) {
                return $Pair_Name;
            }
        }
    }
    return "lost";
}

my %Severity_Val=(
    "High"=>3,
    "Medium"=>2,
    "Low"=>1,
    "Safe"=>-1
);

sub isRecurType($$)
{
    foreach (@RecurTypes)
    {
        if($_->{"Tid1"} eq $_[0]
        and $_->{"Tid2"} eq $_[1])
        {
            return 1;
        }
    }
    return 0;
}

sub pushType($$)
{
    my %TypeDescriptor=(
        "Tid1"  => $_[0],
        "Tid2"  => $_[1]  );
    push(@RecurTypes, \%TypeDescriptor);
}

sub get_SFormat($)
{
    my $Name = $_[0];
    $Name=~s/\./\//g;
    return $Name;
}

sub get_PFormat($)
{
    my $Name = $_[0];
    $Name=~s/\//./g;
    return $Name;
}

sub get_ConstantValue($$)
{
    my ($Value, $ValueType) = @_;
    return "" if(not $Value);
    if($Value eq "\@EMPTY_STRING\@") {
        return "\"\"";
    }
    elsif($ValueType eq "java.lang.String") {
        return "\"".$Value."\"";
    }
    else {
        return $Value;
    }
}

sub getInvoked($)
{
    my $TName = $_[0];
    
    if(my @Invoked = sort keys(%{$ClassMethod_AddedUsed{$TName}}))
    {
        my $MFirst = $Invoked[0];
        my $MSignature = unmangle($MFirst);
        $MSignature=~s/\A.+\.(\w+\()/$1/g; # short name
        my $InvokedBy = $ClassMethod_AddedUsed{$TName}{$MFirst};
        return ($MSignature, $InvokedBy);
    }
    
    return ();
}

sub mergeTypes($$)
{
    my ($Type1_Id, $Type2_Id) = @_;
    return {} if(not $Type1_Id or not $Type2_Id);
    
    if(defined $Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id})
    { # already merged
        return $Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id};
    }
    
    my %Type1 = get_Type($Type1_Id, 1);
    my %Type2 = get_Type($Type2_Id, 2);
    if(isRecurType($Type1_Id, $Type2_Id))
    { # do not follow to recursive declarations
        return {};
    }
    return {} if(not $Type1{"Name"} or not $Type2{"Name"});
    return {} if(not $Type1{"Archive"} or not $Type2{"Archive"});
    return {} if($Type1{"Name"} ne $Type2{"Name"});
    
    if(not classFilter(\%Type1, 1, 0)) {
        return {};
    }
    
    $CheckedTypes{$Type1{"Name"}} = 1;
    
    my %SubProblems = ();
    
    if($Type1{"BaseType"} and $Type2{"BaseType"})
    { # check base type (arrays)
        return mergeTypes($Type1{"BaseType"}, $Type2{"BaseType"});
    }
    
    if($Type2{"Type"}!~/(class|interface)/) {
        return {};
    }
    
    if($Type1{"Type"} eq "class" and not $Class_Constructed{1}{$Type1_Id})
    { # class cannot be constructed or inherited by clients
        return {};
    }
    
    if($Type1{"Type"} eq "class"
    and $Type2{"Type"} eq "interface")
    {
        %{$SubProblems{"Class_Became_Interface"}{""}}=(
            "Type_Name"=>$Type1{"Name"}  );
        
        return ($Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id} = \%SubProblems);
    }
    if($Type1{"Type"} eq "interface"
    and $Type2{"Type"} eq "class")
    {
        %{$SubProblems{"Interface_Became_Class"}{""}}=(
            "Type_Name"=>$Type1{"Name"}  );
        
        return ($Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id} = \%SubProblems);
    }
    if(not $Type1{"Final"}
    and $Type2{"Final"})
    {
        %{$SubProblems{"Class_Became_Final"}{""}}=(
            "Type_Name"=>$Type1{"Name"},
            "Target"=>$Type1{"Name"}  );
    }
    if(not $Type1{"Abstract"}
    and $Type2{"Abstract"})
    {
        %{$SubProblems{"Class_Became_Abstract"}{""}}=(
            "Type_Name"=>$Type1{"Name"}  );
    }
    
    pushType($Type1_Id, $Type2_Id);
    
    foreach my $AddedMethod (keys(%{$AddedMethod_Abstract{$Type1{"Name"}}}))
    {
        if($Type1{"Type"} eq "class")
        {
            if($Type1{"Abstract"})
            {
                if(my @InvokedBy = sort keys(%{$MethodUsed{2}{$AddedMethod}}))
                {
                    %{$SubProblems{"Abstract_Class_Added_Abstract_Method_Invoked_By_Others"}{get_SFormat($AddedMethod)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>$Type1{"Type"},
                        "Target"=>$AddedMethod,
                        "InvokedBy"=>$InvokedBy[0]  );
                }
                else
                {
                    %{$SubProblems{"Abstract_Class_Added_Abstract_Method"}{get_SFormat($AddedMethod)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>$Type1{"Type"},
                        "Target"=>$AddedMethod  );
                }
            }
            else
            {
                %{$SubProblems{"NonAbstract_Class_Added_Abstract_Method"}{get_SFormat($AddedMethod)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>$Type1{"Type"},
                    "Target"=>$AddedMethod  );
            }
        }
        else
        {
            if(my @InvokedBy = sort keys(%{$MethodUsed{2}{$AddedMethod}}))
            {
                %{$SubProblems{"Interface_Added_Abstract_Method_Invoked_By_Others"}{get_SFormat($AddedMethod)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>$Type1{"Type"},
                    "Target"=>$AddedMethod,
                    "InvokedBy"=>$InvokedBy[0]  );
            }
            else
            {
                %{$SubProblems{"Interface_Added_Abstract_Method"}{get_SFormat($AddedMethod)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>$Type1{"Type"},
                    "Target"=>$AddedMethod  );
            }
        }
    }
    foreach my $RemovedMethod (keys(%{$RemovedMethod_Abstract{$Type1{"Name"}}}))
    {
        if($Type1{"Type"} eq "class")
        {
            %{$SubProblems{"Class_Removed_Abstract_Method"}{get_SFormat($RemovedMethod)}} = (
                "Type_Name"=>$Type1{"Name"},
                "Type_Type"=>$Type1{"Type"},
                "Target"=>$RemovedMethod  );
        }
        else
        {
            %{$SubProblems{"Interface_Removed_Abstract_Method"}{get_SFormat($RemovedMethod)}} = (
                "Type_Name"=>$Type1{"Name"},
                "Type_Type"=>$Type1{"Type"},
                "Target"=>$RemovedMethod  );
        }
    }
    if($Type1{"Type"} eq "class"
    and $Type2{"Type"} eq "class")
    {
        my %SuperClass1 = get_Type($Type1{"SuperClass"}, 1);
        my %SuperClass2 = get_Type($Type2{"SuperClass"}, 2);
        if($SuperClass2{"Name"} ne $SuperClass1{"Name"})
        {
            if($SuperClass1{"Name"} eq "java.lang.Object"
            or not $SuperClass1{"Name"})
            {
              # Java 6: java.lang.Object
              # Java 7: none
                if($SuperClass2{"Name"} ne "java.lang.Object")
                {
                    if($SuperClass2{"Abstract"}
                    and $Type1{"Abstract"} and $Type2{"Abstract"}
                    and keys(%{$Class_AbstractMethods{2}{$SuperClass2{"Name"}}}))
                    {
                        if(my ($Invoked, $InvokedBy) = getInvoked($Type1{"Name"}))
                        {
                            %{$SubProblems{"Abstract_Class_Added_Super_Abstract_Class_Invoked_By_Others"}{""}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperClass2{"Name"},
                                "Invoked"=>$Invoked,
                                "InvokedBy"=>$InvokedBy  );
                        }
                        else
                        {
                            %{$SubProblems{"Abstract_Class_Added_Super_Abstract_Class"}{""}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperClass2{"Name"}  );
                        }
                    }
                    else
                    {
                        %{$SubProblems{"Added_Super_Class"}{""}} = (
                            "Type_Name"=>$Type1{"Name"},
                            "Target"=>$SuperClass2{"Name"}  );
                    }
                }
            }
            elsif($SuperClass2{"Name"} eq "java.lang.Object"
            or not $SuperClass2{"Name"})
            {
              # Java 6: java.lang.Object
              # Java 7: none
                if($SuperClass1{"Name"} ne "java.lang.Object")
                {
                    %{$SubProblems{"Removed_Super_Class"}{""}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Target"=>$SuperClass1{"Name"}  );
                }
            }
            else
            {
                %{$SubProblems{"Changed_Super_Class"}{""}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Target"=>$SuperClass1{"Name"},
                    "Old_Value"=>$SuperClass1{"Name"},
                    "New_Value"=>$SuperClass2{"Name"}  );
            }
        }
    }
    my %SuperInterfaces_Old = map {get_TypeName($_, 1) => 1} keys(%{$Type1{"SuperInterface"}});
    my %SuperInterfaces_New = map {get_TypeName($_, 2) => 1} keys(%{$Type2{"SuperInterface"}});
    foreach my $SuperInterface (keys(%SuperInterfaces_New))
    {
        if(not $SuperInterfaces_Old{$SuperInterface})
        {
            my $HaveMethods = keys(%{$Class_AbstractMethods{2}{$SuperInterface}});
            my $HaveFields = keys(%{$Class_Fields{2}{$SuperInterface}});
            
            if($Type1{"Type"} eq "interface")
            {
                if($HaveMethods
                or $SuperInterface=~/\Ajava\./)
                {
                    if($HaveMethods and checkDefaultImpl(2, $SuperInterface, $Type2{"Name"}))
                    {
                        %{$SubProblems{"Interface_Added_Super_Interface_With_Implemented_Methods"}{get_SFormat($SuperInterface)}} = (
                            "Type_Name"=>$Type1{"Name"},
                            "Target"=>$SuperInterface  );
                    }
                    else
                    {
                        if(my ($Invoked, $InvokedBy) = getInvoked($Type1{"Name"}))
                        {
                            %{$SubProblems{"Interface_Added_Super_Interface_Used_By_Others"}{get_SFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface,
                                "Invoked"=>$Invoked,
                                "InvokedBy"=>$InvokedBy  );
                        }
                        else
                        {
                            %{$SubProblems{"Interface_Added_Super_Interface"}{get_SFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface  );
                        }
                    }
                }
                elsif($HaveFields)
                {
                    %{$SubProblems{"Interface_Added_Super_Constant_Interface"}{get_SFormat($SuperInterface)}} = (
                        "Type_Name"=>$Type2{"Name"},
                        "Target"=>$SuperInterface  );
                }
                else
                {
                    # empty interface
                }
            }
            else
            {
                if($Type1{"Abstract"} and $Type2{"Abstract"})
                {
                    if($HaveMethods and checkDefaultImpl(2, $SuperInterface, $Type2{"Name"}))
                    {
                        %{$SubProblems{"Abstract_Class_Added_Super_Interface_With_Implemented_Methods"}{get_SFormat($SuperInterface)}} = (
                            "Type_Name"=>$Type1{"Name"},
                            "Target"=>$SuperInterface  );
                    }
                    else
                    {
                        if(my ($Invoked, $InvokedBy) = getInvoked($Type1{"Name"}))
                        {
                            %{$SubProblems{"Abstract_Class_Added_Super_Interface_Invoked_By_Others"}{get_SFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface,
                                "Invoked"=>$Invoked,
                                "InvokedBy"=>$InvokedBy  );
                        }
                        else
                        {
                            %{$SubProblems{"Abstract_Class_Added_Super_Interface"}{get_SFormat($SuperInterface)}} = (
                                "Type_Name"=>$Type1{"Name"},
                                "Target"=>$SuperInterface  );
                        }
                    }
                }
            }
        }
    }
    foreach my $SuperInterface (keys(%SuperInterfaces_Old))
    {
        if(not $SuperInterfaces_New{$SuperInterface})
        {
            my $HaveMethods = keys(%{$Class_AbstractMethods{1}{$SuperInterface}});
            my $HaveFields = keys(%{$Class_Fields{1}{$SuperInterface}});
            
            if($Type1{"Type"} eq "interface")
            {
                if($HaveMethods
                or $SuperInterface=~/\Ajava\./)
                {
                    %{$SubProblems{"Interface_Removed_Super_Interface"}{get_SFormat($SuperInterface)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>"interface",
                        "Target"=>$SuperInterface  );
                }
                elsif($HaveFields)
                {
                    %{$SubProblems{"Interface_Removed_Super_Constant_Interface"}{get_SFormat($SuperInterface)}} = (
                        "Type_Name"=>$Type1{"Name"},
                        "Target"=>$SuperInterface  );
                }
                else {
                    # empty interface
                }
            }
            else
            {
                %{$SubProblems{"Class_Removed_Super_Interface"}{get_SFormat($SuperInterface)}} = (
                    "Type_Name"=>$Type1{"Name"},
                    "Type_Type"=>"class",
                    "Target"=>$SuperInterface  );
            }
        }
    }
    
    foreach my $Field_Name (sort keys(%{$Type1{"Fields"}}))
    {# check older fields
        my $Access1 = $Type1{"Fields"}{$Field_Name}{"Access"};
        if($Access1=~/private/) {
            next;
        }
        
        my $Field_Pos1 = $Type1{"Fields"}{$Field_Name}{"Pos"};
        my $FieldType1_Id = $Type1{"Fields"}{$Field_Name}{"Type"};
        my %FieldType1 = get_Type($FieldType1_Id, 1);
        
        if(not $Type2{"Fields"}{$Field_Name})
        {# removed fields
            my $StraightPair_Name = findFieldPair($Field_Pos1, \%Type2);
            if($StraightPair_Name ne "lost" and not $Type1{"Fields"}{$StraightPair_Name}
            and $FieldType1{"Name"} eq get_TypeName($Type2{"Fields"}{$StraightPair_Name}{"Type"}, 2))
            {
                if(my $Constant = get_ConstantValue($Type1{"Fields"}{$Field_Name}{"Value"}, $FieldType1{"Name"}))
                {
                    %{$SubProblems{"Renamed_Constant_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Old_Value"=>$Field_Name,
                        "New_Value"=>$StraightPair_Name,
                        "Field_Type"=>$FieldType1{"Name"},
                        "Field_Value"=>$Constant  );
                }
                else
                {
                    %{$SubProblems{"Renamed_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Old_Value"=>$Field_Name,
                        "New_Value"=>$StraightPair_Name,
                        "Field_Type"=>$FieldType1{"Name"}  );
                }
            }
            else
            {
                if(my $Constant = get_ConstantValue($Type1{"Fields"}{$Field_Name}{"Value"}, $FieldType1{"Name"}))
                { # has a compile-time constant value
                    %{$SubProblems{"Removed_Constant_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Field_Value"=>$Constant,
                        "Field_Type"=>$FieldType1{"Name"},
                        "Type_Type"=>$Type1{"Type"}  );
                }
                else
                {
                    %{$SubProblems{"Removed_NonConstant_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"},
                        "Type_Type"=>$Type1{"Type"},
                        "Field_Type"=>$FieldType1{"Name"}  );
                }
            }
            next;
        }
        my $FieldType2_Id = $Type2{"Fields"}{$Field_Name}{"Type"};
        my %FieldType2 = get_Type($FieldType2_Id, 2);
        
        if(not $Type1{"Fields"}{$Field_Name}{"Static"}
        and $Type2{"Fields"}{$Field_Name}{"Static"})
        {
            if(not $Type1{"Fields"}{$Field_Name}{"Value"})
            {
                %{$SubProblems{"NonConstant_Field_Became_Static"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Field_Type"=>$FieldType1{"Name"},
                    "Type_Name"=>$Type1{"Name"}  );
            }
        }
        elsif($Type1{"Fields"}{$Field_Name}{"Static"}
        and not $Type2{"Fields"}{$Field_Name}{"Static"})
        {
            if($Type1{"Fields"}{$Field_Name}{"Value"})
            {
                %{$SubProblems{"Constant_Field_Became_NonStatic"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Field_Type"=>$FieldType1{"Name"},
                    "Type_Name"=>$Type1{"Name"}  );
            }
            else
            {
                %{$SubProblems{"NonConstant_Field_Became_NonStatic"}{$Field_Name}}=(
                    "Target"=>$Field_Name,
                    "Field_Type"=>$FieldType1{"Name"},
                    "Type_Name"=>$Type1{"Name"}  );
            }
        }
        if(not $Type1{"Fields"}{$Field_Name}{"Final"}
        and $Type2{"Fields"}{$Field_Name}{"Final"})
        {
            %{$SubProblems{"Field_Became_Final"}{$Field_Name}}=(
                "Target"=>$Field_Name,
                "Field_Type"=>$FieldType1{"Name"},
                "Type_Name"=>$Type1{"Name"}  );
        }
        elsif($Type1{"Fields"}{$Field_Name}{"Final"}
        and not $Type2{"Fields"}{$Field_Name}{"Final"})
        {
            %{$SubProblems{"Field_Became_NonFinal"}{$Field_Name}}=(
                "Target"=>$Field_Name,
                "Field_Type"=>$FieldType1{"Name"},
                "Type_Name"=>$Type1{"Name"}  );
        }
        my $Access2 = $Type2{"Fields"}{$Field_Name}{"Access"};
        if($Access1 eq "public" and $Access2=~/protected|private/
        or $Access1 eq "protected" and $Access2=~/private/)
        {
            %{$SubProblems{"Changed_Field_Access"}{$Field_Name}}=(
                "Target"=>$Field_Name,
                "Type_Name"=>$Type1{"Name"},
                "Old_Value"=>$Access1,
                "New_Value"=>$Access2  );
        }
        
        my $Value1 = get_ConstantValue($Type1{"Fields"}{$Field_Name}{"Value"}, $FieldType1{"Name"});
        my $Value2 = get_ConstantValue($Type2{"Fields"}{$Field_Name}{"Value"}, $FieldType2{"Name"});
        
        if($Value1 ne $Value2)
        {
            if($Value1 and $Value2)
            {
                if($Type1{"Fields"}{$Field_Name}{"Final"}
                and $Type2{"Fields"}{$Field_Name}{"Final"})
                {
                    if($Field_Name=~/(\A|_)(VERSION|VERNUM|BUILDNUMBER|BUILD)(_|\Z)/i)
                    {
                        %{$SubProblems{"Changed_Final_Version_Field_Value"}{$Field_Name}}=(
                            "Target"=>$Field_Name,
                            "Field_Type"=>$FieldType1{"Name"},
                            "Type_Name"=>$Type1{"Name"},
                            "Old_Value"=>$Value1,
                            "New_Value"=>$Value2  );
                    }
                    else
                    {
                        %{$SubProblems{"Changed_Final_Field_Value"}{$Field_Name}}=(
                            "Target"=>$Field_Name,
                            "Field_Type"=>$FieldType1{"Name"},
                            "Type_Name"=>$Type1{"Name"},
                            "Old_Value"=>$Value1,
                            "New_Value"=>$Value2  );
                    }
                }
            }
        }
        
        my %Sub_SubChanges = detectTypeChange($FieldType1_Id, $FieldType2_Id, "Field");
        foreach my $Sub_SubProblemType (keys(%Sub_SubChanges))
        {
            %{$SubProblems{$Sub_SubProblemType}{$Field_Name}}=(
                "Target"=>$Field_Name,
                "Type_Name"=>$Type1{"Name"});
            
            foreach my $Attr (keys(%{$Sub_SubChanges{$Sub_SubProblemType}}))
            {
                $SubProblems{$Sub_SubProblemType}{$Field_Name}{$Attr} = $Sub_SubChanges{$Sub_SubProblemType}{$Attr};
            }
        }
        
        if($FieldType1_Id and $FieldType2_Id)
        { # check field type change
            my $Sub_SubProblems = mergeTypes($FieldType1_Id, $FieldType2_Id);
            my %DupProblems = ();
            
            foreach my $Sub_SubProblemType (sort keys(%{$Sub_SubProblems}))
            {
                foreach my $Sub_SubLocation (sort {length($a)<=>length($b)} sort keys(%{$Sub_SubProblems->{$Sub_SubProblemType}}))
                {
                    if(not defined $AllAffected)
                    {
                        if(defined $DupProblems{$Sub_SubProblems->{$Sub_SubProblemType}{$Sub_SubLocation}}) {
                            next;
                        }
                    }
                    
                    my $NewLocation = ($Sub_SubLocation)?$Field_Name.".".$Sub_SubLocation:$Field_Name;
                    $SubProblems{$Sub_SubProblemType}{$NewLocation} = $Sub_SubProblems->{$Sub_SubProblemType}{$Sub_SubLocation};
                    
                    if(not defined $AllAffected)
                    {
                        $DupProblems{$Sub_SubProblems->{$Sub_SubProblemType}{$Sub_SubLocation}} = 1;
                    }
                }
            }
            %DupProblems = ();
        }
    }
    
    foreach my $Field_Name (sort keys(%{$Type2{"Fields"}}))
    { # check added fields
        if($Type2{"Fields"}{$Field_Name}{"Access"}=~/private/) {
            next;
        }
        my $FieldPos2 = $Type2{"Fields"}{$Field_Name}{"Pos"};
        my $FieldType2_Id = $Type2{"Fields"}{$Field_Name}{"Type"};
        my %FieldType2 = get_Type($FieldType2_Id, 2);
        
        if(not $Type1{"Fields"}{$Field_Name})
        {# added fields
            my $StraightPair_Name = findFieldPair($FieldPos2, \%Type1);
            if($StraightPair_Name ne "lost" and not $Type2{"Fields"}{$StraightPair_Name}
            and get_TypeName($Type1{"Fields"}{$StraightPair_Name}{"Type"}, 1) eq $FieldType2{"Name"})
            {
                # Already reported as "Renamed_Field" or "Renamed_Constant_Field"
            }
            else
            {
                if($Type1{"Type"} eq "interface")
                {
                    %{$SubProblems{"Interface_Added_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"}  );
                }
                else
                {
                    %{$SubProblems{"Class_Added_Field"}{$Field_Name}}=(
                        "Target"=>$Field_Name,
                        "Type_Name"=>$Type1{"Name"}  );
                }
            }
        }
    }
    
    pop(@RecurTypes);
    return ($Cache{"mergeTypes"}{$Type1_Id}{$Type2_Id} = \%SubProblems);
}

sub checkDefaultImpl($$$)
{ # Check if all abstract methods of the super class have
  # default implementations in the class
    my ($LibVersion, $SuperClassName, $ClassName) = @_;
    
    foreach my $Method (keys(%{$Class_AbstractMethods{$LibVersion}{$SuperClassName}}))
    {
        if(my $Overridden = findMethod_Class($Method, $ClassName, $LibVersion))
        {
            if($MethodInfo{$LibVersion}{$Overridden}{"Abstract"}) {
                return 0;
            }
        }
        else {
            return 0;
        }
    }
    
    return 1;
}

sub unmangle($)
{
    my $Name = $_[0];
    $Name=~s!/!.!g;
    $Name=~s!:\(!(!g;
    $Name=~s!\).+\Z!)!g;
    if($Name=~/\A(.+)\((.+)\)/)
    {
        my ($ShortName, $MangledParams) = ($1, $2);
        my @UnmangledParams = ();
        my ($IsArray, $Shift, $Pos, $CurParam) = (0, 0, 0, "");
        while($Pos<length($MangledParams))
        {
            my $Symbol = substr($MangledParams, $Pos, 1);
            if($Symbol eq "[")
            { # array
                $IsArray = 1;
                $Pos+=1;
            }
            elsif($Symbol eq "L")
            { # class
                if(substr($MangledParams, $Pos+1)=~/\A(.+?);/) {
                    $CurParam = $1;
                    $Shift = length($CurParam)+2;
                }
                if($IsArray) {
                    $CurParam .= "[]";
                }
                $Pos+=$Shift;
                push(@UnmangledParams, $CurParam);
                ($IsArray, $Shift, $CurParam) = (0, 0, "")
            }
            else
            {
                if($Symbol eq "C") {
                    $CurParam = "char";
                }
                elsif($Symbol eq "B") {
                    $CurParam = "byte";
                }
                elsif($Symbol eq "S") {
                    $CurParam = "short";
                }
                elsif($Symbol eq "S") {
                    $CurParam = "short";
                }
                elsif($Symbol eq "I") {
                    $CurParam = "int";
                }
                elsif($Symbol eq "F") {
                    $CurParam = "float";
                }
                elsif($Symbol eq "J") {
                    $CurParam = "long";
                }
                elsif($Symbol eq "D") {
                    $CurParam = "double";
                }
                else {
                    printMsg("INFO", "WARNING: unmangling error");
                }
                if($IsArray) {
                    $CurParam .= "[]";
                }
                $Pos+=1;
                push(@UnmangledParams, $CurParam);
                ($IsArray, $Shift, $CurParam) = (0, 0, "")
            }
        }
        return $ShortName."(".join(", ", @UnmangledParams).")";
    }
    else {
        return $Name;
    }
}

sub get_TypeName($$)
{
    my ($TypeId, $LibVersion) = @_;
    return $TypeInfo{$LibVersion}{$TypeId}{"Name"};
}

sub get_ShortName($$)
{
    my ($TypeId, $LibVersion) = @_;
    my $TypeName = $TypeInfo{$LibVersion}{$TypeId}{"Name"};
    $TypeName=~s/\A.*\.//g;
    return $TypeName;
}

sub get_TypeType($$)
{
    my ($TypeId, $LibVersion) = @_;
    return $TypeInfo{$LibVersion}{$TypeId}{"Type"};
}

sub get_TypeHeader($$)
{
    my ($TypeId, $LibVersion) = @_;
    return $TypeInfo{$LibVersion}{$TypeId}{"Header"};
}

sub get_BaseType($$)
{
    my ($TypeId, $LibVersion) = @_;
    return "" if(not $TypeId);
    if(defined $Cache{"get_BaseType"}{$TypeId}{$LibVersion}) {
        return %{$Cache{"get_BaseType"}{$TypeId}{$LibVersion}};
    }
    return "" if(not $TypeInfo{$LibVersion}{$TypeId});
    my %Type = %{$TypeInfo{$LibVersion}{$TypeId}};
    return %Type if(not $Type{"BaseType"});
    %Type = get_BaseType($Type{"BaseType"}, $LibVersion);
    $Cache{"get_BaseType"}{$TypeId}{$LibVersion} = \%Type;
    return %Type;
}

sub get_OneStep_BaseType($$)
{
    my ($TypeId, $LibVersion) = @_;
    return "" if(not $TypeId);
    return "" if(not $TypeInfo{$LibVersion}{$TypeId});
    my %Type = %{$TypeInfo{$LibVersion}{$TypeId}};
    return %Type if(not $Type{"BaseType"});
    return get_Type($Type{"BaseType"}, $LibVersion);
}

sub get_Type($$)
{
    my ($TypeId, $LibVersion) = @_;
    return "" if(not $TypeId);
    return "" if(not $TypeInfo{$LibVersion}{$TypeId});
    return %{$TypeInfo{$LibVersion}{$TypeId}};
}

sub classFilter($$$)
{
    my ($Class, $LibVersion, $ClassContext) = @_;
    
    if(defined $Class->{"Dep"}) {
        return 0;
    }
    
    my $CName = $Class->{"Name"};
    my $Package = $Class->{"Package"};
    
    if(defined $ClassListPath
    and not defined $ClassList_User{$CName})
    { # user defined classes
        return 0;
    }
    
    if(defined $SkipClassesList
    and defined $SkipClasses{$CName})
    { # user defined classes
        return 0;
    }
    
    if(skipPackage($Package, $LibVersion))
    { # internal packages
        return 0;
    }
    
    if(skipType($CName))
    { # internal types
        return 0;
    }
    
    if($ClassContext)
    {
        my @Ann = ();
        
        if(defined $Class->{"Annotations"}) {
            @Ann = keys(%{$Class->{"Annotations"}});
        }
        
        if(not annotationFilter(\@Ann, $LibVersion)) {
            return 0;
        }
        
        if($ClientPath)
        {
            if(not defined $UsedClasses_Client{$CName}) {
                return 0;
            }
        }
    }
    
    return 1;
}

sub annotationFilter($$)
{
    my ($Ann, $LibVersion) = @_;
    
    if(not defined $CountMethods)
    {
        if(defined $AddedAnnotations and $LibVersion==1) {
            return 1;
        }
        
        if(defined $RemovedAnnotations and $LibVersion==2) {
            return 1;
        }
    }
    
    if($SkipAnnotationsListPath)
    {
        foreach my $Aid (@{$Ann})
        {
            my $AName = $TypeInfo{$LibVersion}{$Aid}{"Name"};
            
            if(defined $SkipAnnotationList_User{$AName}) {
                return 0;
            }
        }
    }
    
    if($AnnotationsListPath)
    {
        my $Annotated = 0;
        
        foreach my $Aid (@{$Ann})
        {
            my $AName = $TypeInfo{$LibVersion}{$Aid}{"Name"};
            
            if(defined $AnnotationList_User{$AName})
            {
                $Annotated = 1;
                last;
            }
        }
        
        if(not $Annotated) {
            return 0;
        }
    }
    
    return 1;
}

sub methodFilter($$)
{
    my ($Method, $LibVersion) = @_;
    
    if(defined $MethodInfo{$LibVersion}{$Method}{"Dep"}) {
        return 0;
    }
    
    if($MethodInfo{$LibVersion}{$Method}{"Access"}=~/private/)
    { # non-public
        return 0;
    }
    
    my $ClassId = $MethodInfo{$LibVersion}{$Method}{"Class"};
    my %Class = get_Type($ClassId, $LibVersion);
    
    if($Class{"Access"}=~/private/)
    { # skip private classes
        return 0;
    }
    
    my $Package = $MethodInfo{$LibVersion}{$Method}{"Package"};
    
    my @Ann = ();
    
    if(defined $Class{"Annotations"}) {
        @Ann = (@Ann, keys(%{$Class{"Annotations"}}));
    }
    
    if(defined $MethodInfo{$LibVersion}{$Method}{"Annotations"}) {
        @Ann = (@Ann, keys(%{$MethodInfo{$LibVersion}{$Method}{"Annotations"}}));
    }
    
    if(not annotationFilter(\@Ann, $LibVersion)) {
        return 0;
    }
    
    if($ClientPath)
    { # user defined application
        if(not defined $UsedMethods_Client{$Method}
        and not defined $UsedClasses_Client{$Class{"Name"}}) {
            return 0;
        }
    }
    
    if(skipPackage($Package, $LibVersion))
    { # internal packages
        return 0;
    }
    
    if(not classFilter(\%Class, $LibVersion, 0)) {
        return 0;
    }
    
    if(defined $SkipDeprecated)
    {
        if($Class{"Deprecated"})
        { # deprecated class
            return 0;
        }
        if($MethodInfo{$LibVersion}{$Method}{"Deprecated"})
        { # deprecated method
            return 0;
        }
    }
    
    return 1;
}

sub skipType($)
{
    my $TName = $_[0];
    
    if(defined $SkipInternalTypes)
    {
        if($TName=~/($SkipInternalTypes)/) {
            return 1;
        }
    }
    
    return 0;
}

sub skipPackage($$)
{
    my ($Package, $LibVersion) = @_;
    return 0 if(not $Package);
    
    if(defined $SkipInternalPackages)
    {
        if($Package=~/($SkipInternalPackages)/) {
            return 1;
        }
    }
    
    if(defined $SkipPackages{$LibVersion})
    {
        foreach my $SkipPackage (keys(%{$SkipPackages{$LibVersion}}))
        {
            if($Package=~/\A\Q$SkipPackage\E(\.|\Z)/)
            { # user skipped packages
                return 1;
            }
        }
    }
    
    if(not defined $KeepInternal)
    {
        my $Note = (not keys %SkippedPackage)?" (use --keep-internal option to check them)":"";
        
        if($Package=~/(\A|\.)(internal|impl|examples)(\.|\Z)/)
        { # internal packages
            if(not $SkippedPackage{$LibVersion}{$2})
            {
                $SkippedPackage{$LibVersion}{$2} = 1;
                printMsg("WARNING", "skip \"$2\" packages".$Note);
            }
            return 1;
        }
    }
    
    if(defined $KeepPackages{$LibVersion}
    and my @Keeped = keys(%{$KeepPackages{$LibVersion}}))
    {
        my $UserKeeped = 0;
        foreach my $KeepPackage (@Keeped)
        {
            if($Package=~/\A\Q$KeepPackage\E(\.|\Z)/)
            { # user keeped packages
                $UserKeeped = 1;
                last;
            }
        }
        if(not $UserKeeped) {
            return 1;
        }
    }
    
    return 0;
}

sub get_MSuffix($)
{
    my $Method = $_[0];
    if($Method=~/(\(.*\))/) {
        return $1;
    }
    return "";
}

sub get_MShort($)
{
    my $Method = $_[0];
    if($Method=~/([^\.]+)\:\(/) {
        return $1;
    }
    return "";
}

sub findMethod($$$$)
{
    my ($Method, $MethodVersion, $ClassName, $ClassVersion) = @_;
    my $ClassId = $TName_Tid{$ClassVersion}{$ClassName};
    
    if($ClassId)
    {
        my @Search = ();
        if(get_TypeType($ClassId, $ClassVersion) eq "class")
        {
            if(my $SuperClassId = $TypeInfo{$ClassVersion}{$ClassId}{"SuperClass"}) {
                push(@Search, $SuperClassId);
            }
        }
        
        if(not defined $MethodInfo{$MethodVersion}{$Method}
        or $MethodInfo{$MethodVersion}{$Method}{"Abstract"})
        {
            if(my @SuperInterfaces = sort keys(%{$TypeInfo{$ClassVersion}{$ClassId}{"SuperInterface"}})) {
                push(@Search, @SuperInterfaces);
            }
        }
        
        foreach my $SuperId (@Search)
        {
            if($SuperId eq $ClassId) {
                next;
            }
            
            my $SuperName = get_TypeName($SuperId, $ClassVersion);
            
            if(my $MethodInClass = findMethod_Class($Method, $SuperName, $ClassVersion)) {
                return $MethodInClass;
            }
            elsif(my $MethodInSuperClasses = findMethod($Method, $MethodVersion, $SuperName, $ClassVersion)) {
                return $MethodInSuperClasses;
            }
        }
    }
    
    my $TargetSuffix = get_MSuffix($Method);
    my $TargetShortName = get_MShort($Method);
    
    # search in java.lang.Object
    foreach my $C (keys(%JavaObjectMethod))
    {
        if($TargetSuffix eq get_MSuffix($C))
        {
            if($TargetShortName eq get_MShort($C)) {
                return $C;
            }
        }
    }
    
    return undef;
}

sub findMethod_Class($$$)
{
    my ($Method, $ClassName, $ClassVersion) = @_;
    my $TargetSuffix = get_MSuffix($Method);
    my $TargetShortName = get_MShort($Method);
    
    if(not defined $Class_Methods{$ClassVersion}{$ClassName}) {
        return undef;
    }
    
    foreach my $Candidate (sort keys(%{$Class_Methods{$ClassVersion}{$ClassName}}))
    { # search for method with the same parameters suffix
        next if($MethodInfo{$ClassVersion}{$Candidate}{"Constructor"});
        if($TargetSuffix eq get_MSuffix($Candidate))
        {
            if($TargetShortName eq get_MShort($Candidate)) {
                return $Candidate;
            }
        }
    }
    
    return undef;
}

sub prepareMethods($)
{
    my $LibVersion = $_[0];
    foreach my $Method (keys(%{$MethodInfo{$LibVersion}}))
    {
        if($MethodInfo{$LibVersion}{$Method}{"Access"}!~/private/)
        {
            if($MethodInfo{$LibVersion}{$Method}{"Constructor"}) {
                registerUsage($MethodInfo{$LibVersion}{$Method}{"Class"}, $LibVersion);
            }
            else {
                registerUsage($MethodInfo{$LibVersion}{$Method}{"Return"}, $LibVersion);
            }
        }
    }
}

sub mergeMethods()
{
    foreach my $Method (sort keys(%{$MethodInfo{1}}))
    { # compare methods
        next if(not defined $MethodInfo{2}{$Method});
        
        if(not $MethodInfo{1}{$Method}{"Archive"}
        or not $MethodInfo{2}{$Method}{"Archive"}) {
            next;
        }
        
        if(not methodFilter($Method, 1)) {
            next;
        }
        
        my $ClassId1 = $MethodInfo{1}{$Method}{"Class"};
        my %Class1 = get_Type($ClassId1, 1);
        
        $CheckedTypes{$Class1{"Name"}} = 1;
        $CheckedMethods{$Method} = 1;
        
        my %Class2 = get_Type($MethodInfo{2}{$Method}{"Class"}, 2);
        if(not $MethodInfo{1}{$Method}{"Static"}
        and $Class1{"Type"} eq "class" and not $Class_Constructed{1}{$ClassId1})
        { # class cannot be constructed or inherited by clients
          # non-static method cannot be called
            next;
        }
        # checking attributes
        if(not $MethodInfo{1}{$Method}{"Static"}
        and $MethodInfo{2}{$Method}{"Static"}) {
            %{$CompatProblems{$Method}{"Method_Became_Static"}{""}} = ();
        }
        elsif($MethodInfo{1}{$Method}{"Static"}
        and not $MethodInfo{2}{$Method}{"Static"}) {
            %{$CompatProblems{$Method}{"Method_Became_NonStatic"}{""}} = ();
        }
        if(not $MethodInfo{1}{$Method}{"Synchronized"}
        and $MethodInfo{2}{$Method}{"Synchronized"}) {
            %{$CompatProblems{$Method}{"Method_Became_Synchronized"}{""}} = ();
        }
        elsif($MethodInfo{1}{$Method}{"Synchronized"}
        and not $MethodInfo{2}{$Method}{"Synchronized"}) {
            %{$CompatProblems{$Method}{"Method_Became_NonSynchronized"}{""}} = ();
        }
        if(not $MethodInfo{1}{$Method}{"Final"}
        and $MethodInfo{2}{$Method}{"Final"})
        {
            if($MethodInfo{1}{$Method}{"Static"}) {
                %{$CompatProblems{$Method}{"Static_Method_Became_Final"}{""}} = ();
            }
            else {
                %{$CompatProblems{$Method}{"NonStatic_Method_Became_Final"}{""}} = ();
            }
        }
        my $Access1 = $MethodInfo{1}{$Method}{"Access"};
        my $Access2 = $MethodInfo{2}{$Method}{"Access"};
        if($Access1 eq "public" and $Access2=~/protected|private/
        or $Access1 eq "protected" and $Access2=~/private/)
        {
            %{$CompatProblems{$Method}{"Changed_Method_Access"}{""}} = (
                "Old_Value"=>$Access1,
                "New_Value"=>$Access2  );
        }
        if($Class1{"Type"} eq "class"
        and $Class2{"Type"} eq "class")
        {
            if(not $MethodInfo{1}{$Method}{"Abstract"}
            and $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_Abstract"}{""}} = ();
                %{$CompatProblems{$Method}{"Class_Method_Became_Abstract"}{"this.".get_SFormat($Method)}} = (
                    "Type_Name"=>$Class1{"Name"},
                    "Target"=>$Method  );
            }
            elsif($MethodInfo{1}{$Method}{"Abstract"}
            and not $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_NonAbstract"}{""}} = ();
                %{$CompatProblems{$Method}{"Class_Method_Became_NonAbstract"}{"this.".get_SFormat($Method)}} = (
                    "Type_Name"=>$Class1{"Name"},
                    "Target"=>$Method  );
            }
        }
        elsif($Class1{"Type"} eq "interface"
        and $Class2{"Type"} eq "interface")
        {
            if(not $MethodInfo{1}{$Method}{"Abstract"}
            and $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_NonDefault"}{""}} = ();
                %{$CompatProblems{$Method}{"Interface_Method_Became_NonDefault"}{"this.".get_SFormat($Method)}} = (
                    "Type_Name"=>$Class1{"Name"},
                    "Target"=>$Method  );
            }
            elsif($MethodInfo{1}{$Method}{"Abstract"}
            and not $MethodInfo{2}{$Method}{"Abstract"})
            {
                %{$CompatProblems{$Method}{"Method_Became_Default"}{""}} = ();
                %{$CompatProblems{$Method}{"Interface_Method_Became_Default"}{"this.".get_SFormat($Method)}} = (
                    "Type_Name"=>$Class1{"Name"},
                    "Target"=>$Method  );
            }
        }
        
        my %Exceptions_Old = map {get_TypeName($_, 1) => $_} keys(%{$MethodInfo{1}{$Method}{"Exceptions"}});
        my %Exceptions_New = map {get_TypeName($_, 2) => $_} keys(%{$MethodInfo{2}{$Method}{"Exceptions"}});
        foreach my $Exception (keys(%Exceptions_Old))
        {
            if(not $Exceptions_New{$Exception})
            {
                my %ExceptionType = get_Type($Exceptions_Old{$Exception}, 1);
                my $SuperClass = $ExceptionType{"SuperClass"};
                if($KnownRuntimeExceptions{$Exception}
                or defined $SuperClass and get_TypeName($SuperClass, 1) eq "java.lang.RuntimeException")
                {
                    if(not $MethodInfo{1}{$Method}{"Abstract"}
                    and not $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Removed_Unchecked_Exception"}{"this.".get_SFormat($Exception)}} = (
                            "Type_Name"=>$Class1{"Name"},
                            "Target"=>$Exception  );
                    }
                }
                else
                {
                    if($MethodInfo{1}{$Method}{"Abstract"}
                    and $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Abstract_Method_Removed_Checked_Exception"}{"this.".get_SFormat($Exception)}} = (
                            "Type_Name"=>$Class1{"Name"},
                            "Target"=>$Exception  );
                    }
                    else
                    {
                        %{$CompatProblems{$Method}{"NonAbstract_Method_Removed_Checked_Exception"}{"this.".get_SFormat($Exception)}} = (
                            "Type_Name"=>$Class1{"Name"},
                            "Target"=>$Exception  );
                    }
                }
            }
        }
        foreach my $Exception (keys(%Exceptions_New))
        {
            if(not $Exceptions_Old{$Exception})
            {
                my %ExceptionType = get_Type($Exceptions_New{$Exception}, 2);
                my $SuperClass = $ExceptionType{"SuperClass"};
                if($KnownRuntimeExceptions{$Exception}
                or defined $SuperClass and get_TypeName($SuperClass, 2) eq "java.lang.RuntimeException")
                {
                    if(not $MethodInfo{1}{$Method}{"Abstract"}
                    and not $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Added_Unchecked_Exception"}{"this.".get_SFormat($Exception)}} = (
                            "Type_Name"=>$Class1{"Name"},
                            "Target"=>$Exception  );
                    }
                }
                else
                {
                    if($MethodInfo{1}{$Method}{"Abstract"}
                    and $MethodInfo{2}{$Method}{"Abstract"})
                    {
                        %{$CompatProblems{$Method}{"Abstract_Method_Added_Checked_Exception"}{"this.".get_SFormat($Exception)}} = (
                            "Type_Name"=>$Class1{"Name"},
                            "Target"=>$Exception  );
                    }
                    else
                    {
                        %{$CompatProblems{$Method}{"NonAbstract_Method_Added_Checked_Exception"}{"this.".get_SFormat($Exception)}} = (
                            "Type_Name"=>$Class1{"Name"},
                            "Target"=>$Exception  );
                    }
                }
            }
        }
        
        if(defined $MethodInfo{1}{$Method}{"Param"})
        {
            foreach my $ParamPos (sort {int($a) <=> int($b)} keys(%{$MethodInfo{1}{$Method}{"Param"}}))
            { # checking parameters
                mergeParameters($Method, $ParamPos, $ParamPos);
            }
        }
        
        # check object type
        my $ObjectType1_Id = $MethodInfo{1}{$Method}{"Class"};
        my $ObjectType2_Id = $MethodInfo{2}{$Method}{"Class"};
        if($ObjectType1_Id and $ObjectType2_Id)
        {
            my $SubProblems = mergeTypes($ObjectType1_Id, $ObjectType2_Id);
            foreach my $SubProblemType (keys(%{$SubProblems}))
            {
                foreach my $SubLocation (keys(%{$SubProblems->{$SubProblemType}}))
                {
                    my $NewLocation = ($SubLocation)?"this.".$SubLocation:"this";
                    $CompatProblems{$Method}{$SubProblemType}{$NewLocation} = $SubProblems->{$SubProblemType}{$SubLocation};
                }
            }
        }
        # check return type
        my $ReturnType1_Id = $MethodInfo{1}{$Method}{"Return"};
        my $ReturnType2_Id = $MethodInfo{2}{$Method}{"Return"};
        if($ReturnType1_Id and $ReturnType2_Id)
        {
            my $SubProblems = mergeTypes($ReturnType1_Id, $ReturnType2_Id);
            foreach my $SubProblemType (keys(%{$SubProblems}))
            {
                foreach my $SubLocation (keys(%{$SubProblems->{$SubProblemType}}))
                {
                    my $NewLocation = ($SubLocation)?"retval.".$SubLocation:"retval";
                    $CompatProblems{$Method}{$SubProblemType}{$NewLocation} = $SubProblems->{$SubProblemType}{$SubLocation};
                }
            }
        }
    }
}

sub mergeParameters($$$)
{
    my ($Method, $ParamPos1, $ParamPos2) = @_;
    if(not $Method or not defined $MethodInfo{1}{$Method}{"Param"}
    or not defined $MethodInfo{2}{$Method}{"Param"}) {
        return;
    }
    
    my $ParamType1_Id = $MethodInfo{1}{$Method}{"Param"}{$ParamPos1}{"Type"};
    my $ParamType2_Id = $MethodInfo{2}{$Method}{"Param"}{$ParamPos2}{"Type"};
    
    if(not $ParamType1_Id or not $ParamType2_Id) {
        return;
    }
    
    my $Parameter_Name = $MethodInfo{1}{$Method}{"Param"}{$ParamPos1}{"Name"};
    my $Parameter_Location = ($Parameter_Name)?$Parameter_Name:showPos($ParamPos1)." Parameter";
    
    # checking type declaration changes
    my $SubProblems = mergeTypes($ParamType1_Id, $ParamType2_Id);
    foreach my $SubProblemType (keys(%{$SubProblems}))
    {
        foreach my $SubLocation (keys(%{$SubProblems->{$SubProblemType}}))
        {
            my $NewLocation = ($SubLocation)?$Parameter_Location.".".$SubLocation:$Parameter_Location;
            $CompatProblems{$Method}{$SubProblemType}{$NewLocation} = $SubProblems->{$SubProblemType}{$SubLocation};
        }
    }
}

sub detectTypeChange($$$)
{
    my ($Type1_Id, $Type2_Id, $Prefix) = @_;
    my %LocalProblems = ();
    my %Type1 = get_Type($Type1_Id, 1);
    my %Type2 = get_Type($Type2_Id, 2);
    my %Type1_Base = ($Type1{"Type"} eq "array")?get_OneStep_BaseType($Type1_Id, 1):get_BaseType($Type1_Id, 1);
    my %Type2_Base = ($Type2{"Type"} eq "array")?get_OneStep_BaseType($Type2_Id, 2):get_BaseType($Type2_Id, 2);
    return () if(not $Type1{"Name"} or not $Type2{"Name"});
    return () if(not $Type1_Base{"Name"} or not $Type2_Base{"Name"});
    if($Type1_Base{"Name"} ne $Type2_Base{"Name"} and $Type1{"Name"} eq $Type2{"Name"})
    {# base type change
        %{$LocalProblems{"Changed_".$Prefix."_BaseType"}}=(
            "Old_Value"=>$Type1_Base{"Name"},
            "New_Value"=>$Type2_Base{"Name"} );
    }
    elsif($Type1{"Name"} ne $Type2{"Name"})
    {# type change
        %{$LocalProblems{"Changed_".$Prefix."_Type"}}=(
            "Old_Value"=>$Type1{"Name"},
            "New_Value"=>$Type2{"Name"} );
    }
    return %LocalProblems;
}

sub htmlSpecChars($)
{
    my $Str = $_[0];
    if(not defined $Str
    or $Str eq "") {
        return "";
    }
    $Str=~s/\&([^#]|\Z)/&amp;$1/g;
    $Str=~s/</&lt;/g;
    $Str=~s/\-\>/&#45;&gt;/g; # &minus;
    $Str=~s/>/&gt;/g;
    $Str=~s/([^ ])( )([^ ])/$1\@ALONE_SP\@$3/g;
    $Str=~s/ /&#160;/g; # &nbsp;
    $Str=~s/\@ALONE_SP\@/ /g;
    $Str=~s/\n/<br\/>/g;
    $Str=~s/\"/&quot;/g;
    $Str=~s/\'/&#39;/g;
    return $Str;
}

sub black_Name($$)
{
    my ($M, $V) = @_;
    return "<span class='iname_b'>".highLight_Signature($M, $V)."</span>";
}

sub black_Name_S($)
{
    my $Name = $_[0];
    $Name=~s!\A(\w+)!<span class='iname_b'>$1</span>&#160;!g;
    return $Name;
}

sub highLight_Signature($$)
{
    my ($M, $V) = @_;
    return get_Signature($M, $V, "Class|HTML|Italic");
}

sub highLight_Signature_Italic_Color($$)
{
    my ($M, $V) = @_;
    return get_Signature($M, $V, "Full|HTML|Italic|Color");
}

sub get_Signature($$$)
{
    my ($Method, $LibVersion, $Kind) = @_;
    if(defined $Cache{"get_Signature"}{$LibVersion}{$Method}{$Kind}) {
        return $Cache{"get_Signature"}{$LibVersion}{$Method}{$Kind};
    }
    
    # settings
    my ($Full, $Html, $Simple, $Italic, $Color,
    $ShowParams, $ShowClass, $ShowAttr, $Target) = (0, 0, 0, 0, 0, 0, 0, 0, undef);
    
    if($Kind=~/Full/) {
        $Full = 1;
    }
    if($Kind=~/HTML/) {
        $Html = 1;
    }
    if($Kind=~/Simple/) {
        $Simple = 1;
    }
    if($Kind=~/Italic/) {
        $Italic = 1;
    }
    if($Kind=~/Color/) {
        $Color = 1;
    }
    if($Kind=~/Target=(\d+)/) {
        $Target = $1;
    }
    if($Kind=~/Param/) {
        $ShowParams = 1;
    }
    if($Kind=~/Class/) {
        $ShowClass = 1;
    }
    if($Kind=~/Attr/) {
        $ShowAttr = 1;
    }
    
    if(not defined $MethodInfo{$LibVersion}{$Method}{"ShortName"})
    { # from java.lang.Object
        if($Html or $Simple) {
            return htmlSpecChars($Method);
        }
        else
        {
            return $Method;
        }
    }
    
    my $Signature = $MethodInfo{$LibVersion}{$Method}{"ShortName"};
    if($Full or $ShowClass)
    {
        my $Class = get_TypeName($MethodInfo{$LibVersion}{$Method}{"Class"}, $LibVersion);
        
        if($HideTemplates) {
            $Class=~s/<.*>//g;
        }
        
        if($Html) {
            $Class = htmlSpecChars($Class);
        }
        
        $Signature = $Class.".".$Signature;
    }
    my @Params = ();
    
    if(defined $MethodInfo{$LibVersion}{$Method}{"Param"})
    {
        foreach my $PPos (sort {int($a)<=>int($b)}
        keys(%{$MethodInfo{$LibVersion}{$Method}{"Param"}}))
        {
            my $PTid = $MethodInfo{$LibVersion}{$Method}{"Param"}{$PPos}{"Type"};
            if(my $PTName = get_TypeName($PTid, $LibVersion))
            {
                if($HideTemplates) {
                    $PTName=~s/<.*>//g;
                }
                
                if(not $ShowPackages) {
                    $PTName=~s/(\A|\<\s*|\,\s*)[a-z0-9\.]+\./$1/g;
                }
                
                if($Html) {
                    $PTName = htmlSpecChars($PTName);
                }
                
                if($Full or $ShowParams)
                {
                    my $PName = $MethodInfo{$LibVersion}{$Method}{"Param"}{$PPos}{"Name"};
                    
                    if($Simple) {
                        $PName = "<i>$PName</i>";
                    }
                    elsif($Html)
                    {
                        my $Style = "param";
                        
                        if(defined $Target
                        and $Target==$PPos) {
                            $PName = "<span class='focus_p'>$PName</span>";
                        }
                        elsif($Color) {
                            $PName = "<span class='color_p'>$PName</span>";
                        }
                        else {
                            $PName = "<i>$PName</i>";
                        }
                    }
                    
                    push(@Params, $PTName." ".$PName);
                }
                else {
                    push(@Params, $PTName);
                }
            }
        }
    }
    
    if($Simple) {
        $Signature = "<b>".$Signature."</b>";
    }
    
    if($Html and not $Simple)
    {
        $Signature .= "&#160;";
        $Signature .= "<span class='sym_p'>";
        if(@Params)
        {
            foreach my $Pos (0 .. $#Params)
            {
                my $Name = "";
                
                if($Pos==0) {
                    $Name .= "(&#160;";
                }
                
                $Name .= $Params[$Pos];
                
                $Name = "<span>".$Name."</span>";
                
                if($Pos==$#Params) {
                    $Name .= "&#160;)";
                }
                else {
                    $Name .= ", ";
                }
                
                $Signature .= $Name;
            }
        }
        else {
            $Signature .= "(&#160;)";
        }
        $Signature .= "</span>";
    }
    else
    {
        if(@Params) {
            $Signature .= " ( ".join(", ", @Params)." )";
        }
        else {
            $Signature .= " ( )";
        }
    }
    
    if($Full or $ShowAttr)
    {
        if($MethodInfo{$LibVersion}{$Method}{"Static"}) {
            $Signature .= " [static]";
        }
        elsif($MethodInfo{$LibVersion}{$Method}{"Abstract"}) {
            $Signature .= " [abstract]";
        }
    }
    
    if($Full)
    {
        if($ShowAccess)
        {
            if(my $Access = $MethodInfo{$LibVersion}{$Method}{"Access"})
            {
                if($Access ne "public") {
                    $Signature .= " [".$Access."]";
                }
            }
        }
        
        if(my $ReturnId = $MethodInfo{$LibVersion}{$Method}{"Return"})
        {
            my $RName = get_TypeName($ReturnId, $LibVersion);
            
            if($HideTemplates) {
                $RName=~s/<.*>//g;
            }
            
            if(not $ShowPackages) {
                $RName=~s/(\A|\<\s*|\,\s*)[a-z0-9\.]+\./$1/g;
            }
            
            if($Simple) {
                $Signature .= " <b>:</b> ".htmlSpecChars($RName);
            }
            elsif($Html) {
                $Signature .= "<span class='sym_p nowrap'> &#160;<b>:</b>&#160;&#160;".htmlSpecChars($RName)."</span>";
            }
            else {
                $Signature .= " : ".$RName;
            }
        }
        
        if(not $SkipDeprecated)
        {
            if($MethodInfo{$LibVersion}{$Method}{"Deprecated"}) {
                $Signature .= " *DEPRECATED*";
            }
        }
    }
    
    $Signature=~s/java\.lang\.//g;
    
    if($Html)
    {
        if(not $SkipDeprecated) {
            $Signature=~s!(\*deprecated\*)!<span class='deprecated'>$1</span>!ig;
        }
        
        $Signature=~s!(\[static\]|\[abstract\]|\[public\]|\[private\]|\[protected\])!<span class='attr'>$1</span>!g;
    }
    
    if($Simple) {
        $Signature=~s/\[\]/\[ \]/g;
    }
    elsif($Html)
    {
        $Signature=~s!\[\]![&#160;]!g;
        $Signature=~s!operator=!operator&#160;=!g;
    }
    
    return ($Cache{"get_Signature"}{$LibVersion}{$Method}{$Kind} = $Signature);
}

sub checkJavaCompiler($)
{ # check javac: compile simple program
    my $Cmd = $_[0];
    
    if(not $Cmd) {
        return;
    }
    
    writeFile($TMP_DIR."/test_javac/Simple.java",
    "public class Simple {
        public Integer f;
        public void method(Integer p) { };
    }");
    chdir($TMP_DIR."/test_javac");
    system("$Cmd Simple.java 2>errors.txt");
    chdir($ORIG_DIR);
    if($?)
    {
        my $Msg = "something is going wrong with the Java compiler (javac):\n";
        my $Err = readFile($TMP_DIR."/test_javac/errors.txt");
        $Msg .= $Err;
        if($Err=~/elf\/start\.S/ and $Err=~/undefined\s+reference\s+to/)
        { # /usr/lib/gcc/i586-suse-linux/4.5/../../../crt1.o: In function _start:
          # /usr/src/packages/BUILD/glibc-2.11.3/csu/../sysdeps/i386/elf/start.S:115: undefined reference to main
            $Msg .= "\nDid you install a JDK-devel package?";
        }
        exitStatus("Error", $Msg);
    }
}

sub runTests($$$$)
{
    my ($TestsPath, $PackageName, $Path_v1, $Path_v2) = @_;
    # compile with old version of package
    my $JavacCmd = get_CmdPath("javac");
    if(not $JavacCmd) {
        exitStatus("Not_Found", "can't find \"javac\" compiler");
    }
    my $JavaCmd = get_CmdPath("java");
    if(not $JavaCmd) {
        exitStatus("Not_Found", "can't find \"java\" command");
    }
    mkpath($TestsPath."/$PackageName/");
    foreach my $ClassPath (cmd_find($Path_v1,"","*\.class",""))
    {# create a compile-time package copy
        copy($ClassPath, $TestsPath."/$PackageName/");
    }
    chdir($TestsPath);
    system($JavacCmd." -g *.java");
    chdir($ORIG_DIR);
    foreach my $TestSrc (cmd_find($TestsPath,"","*\.java",""))
    { # remove test source
        unlink($TestSrc);
    }
    
    my $PkgPath = $TestsPath."/".$PackageName;
    if(-d $PkgPath) {
        rmtree($PkgPath);
    }
    mkpath($PkgPath);
    foreach my $ClassPath (cmd_find($Path_v2,"","*\.class",""))
    {# create a run-time package copy
        copy($ClassPath, $PkgPath."/");
    }
    my $TEST_REPORT = "";
    foreach my $TestPath (cmd_find($TestsPath,"","*\.class",1))
    {# run tests
        my $Name = get_filename($TestPath);
        $Name=~s/\.class\Z//g;
        chdir($TestsPath);
        system($JavaCmd." $Name >result.txt 2>&1");
        chdir($ORIG_DIR);
        my $Result = readFile($TestsPath."/result.txt");
        unlink($TestsPath."/result.txt");
        $TEST_REPORT .= "TEST CASE: $Name\n";
        if($Result) {
            $TEST_REPORT .= "RESULT: FAILED\n";
            $TEST_REPORT .= "OUTPUT:\n$Result\n";
        }
        else {
            $TEST_REPORT .= "RESULT: SUCCESS\n";
        }
        $TEST_REPORT .= "\n";
    }
    writeFile("$TestsPath/Journal.txt", $TEST_REPORT);
    
    if(-d $PkgPath) {
        rmtree($PkgPath);
    }
}

sub compileJavaLib($$$)
{
    my ($LibName, $BuildRoot1, $BuildRoot2) = @_;
    my $JavacCmd = get_CmdPath("javac");
    if(not $JavacCmd) {
        exitStatus("Not_Found", "can't find \"javac\" compiler");
    }
    checkJavaCompiler($JavacCmd);
    my $JarCmd = get_CmdPath("jar");
    if(not $JarCmd) {
        exitStatus("Not_Found", "can't find \"jar\" command");
    }
    writeFile("$BuildRoot1/MANIFEST.MF", "Implementation-Version: 1.0\n");
    # space before value, new line
    writeFile("$BuildRoot2/MANIFEST.MF", "Implementation-Version: 2.0\n");
    my (%SrcDir1, %SrcDir2) = ();
    foreach my $Path (cmd_find($BuildRoot1,"f","*.java","")) {
        $SrcDir1{get_dirname($Path)} = 1;
    }
    foreach my $Path (cmd_find($BuildRoot2,"f","*.java","")) {
        $SrcDir2{get_dirname($Path)} = 1;
    }
    # build classes v.1
    foreach my $Dir (keys(%SrcDir1))
    {
        chdir($Dir);
        system("$JavacCmd -g *.java");
        chdir($ORIG_DIR);
        if($?) {
            exitStatus("Error", "can't compile classes v.1");
        }
    }
    # create java archive v.1
    chdir($BuildRoot1);
    system("$JarCmd -cmf MANIFEST.MF $LibName.jar TestPackage");
    chdir($ORIG_DIR);
    
    # build classes v.2
    foreach my $Dir (keys(%SrcDir2))
    {
        chdir($Dir);
        system("$JavacCmd -g *.java");
        chdir($ORIG_DIR);
        if($?) {
            exitStatus("Error", "can't compile classes v.2");
        }
    }
    # create java archive v.2
    chdir($BuildRoot2);
    system("$JarCmd -cmf MANIFEST.MF $LibName.jar TestPackage");
    chdir($ORIG_DIR);
    
    foreach my $SrcPath (cmd_find($BuildRoot1,"","*\.java","")) {
        unlink($SrcPath);
    }
    foreach my $SrcPath (cmd_find($BuildRoot2,"","*\.java","")) {
        unlink($SrcPath);
    }
    return 1;
}

sub readLineNum($$)
{
    my ($Path, $Num) = @_;
    return "" if(not $Path or not -f $Path);
    open (FILE, $Path);
    foreach (1 ... $Num) {
        <FILE>;
    }
    my $Line = <FILE>;
    close(FILE);
    return $Line;
}

sub readAttributes($$)
{
    my ($Path, $Num) = @_;
    return () if(not $Path or not -f $Path);
    my %Attributes = ();
    if(readLineNum($Path, $Num)=~/<!--\s+(.+)\s+-->/)
    {
        foreach my $AttrVal (split(/;/, $1))
        {
            if($AttrVal=~/(.+):(.+)/)
            {
                my ($Name, $Value) = ($1, $2);
                $Attributes{$Name} = $Value;
            }
        }
    }
    return \%Attributes;
}

sub runChecker($$$)
{
    my ($LibName, $Path1, $Path2) = @_;
    writeFile("$LibName/v1.xml", "
        <version>
            1.0
        </version>
        <archives>
            ".get_abs_path($Path1)."
        </archives>");
    writeFile("$LibName/v2.xml", "
        <version>
            2.0
        </version>
        <archives>
            ".get_abs_path($Path2)."
        </archives>");
    my $Cmd = "perl $0 -l $LibName $LibName/v1.xml $LibName/v2.xml";
    if($Quick) {
        $Cmd .= " -quick";
    }
    if(defined $SkipDeprecated) {
        $Cmd .= " -skip-deprecated";
    }
    if(defined $OldStyle) {
        $Cmd .= " -old-style";
    }
    writeFile($TMP_DIR."/skip-annotations.list", "TestPackage.Beta");
    $Cmd .= " -skip-annotations-list ".$TMP_DIR."/skip-annotations.list";
    if($Debug)
    {
        $Cmd .= " -debug";
        printMsg("INFO", "running $Cmd");
    }
    system($Cmd);
    my $Report = "compat_reports/$LibName/1.0_to_2.0/compat_report.html";
    # Binary
    my $BReport = readAttributes($Report, 0);
    my $NProblems = $BReport->{"type_problems_high"}+$BReport->{"type_problems_medium"};
    $NProblems += $BReport->{"method_problems_high"}+$BReport->{"method_problems_medium"};
    $NProblems += $BReport->{"removed"};
    # Source
    my $SReport = readAttributes($Report, 1);
    $NProblems += $SReport->{"type_problems_high"}+$SReport->{"type_problems_medium"};
    $NProblems += $SReport->{"method_problems_high"}+$SReport->{"method_problems_medium"};
    $NProblems += $SReport->{"removed"};
    if($NProblems>=100) {
        printMsg("INFO", "test result: SUCCESS ($NProblems breaks found)\n");
    }
    else {
        printMsg("ERROR", "test result: FAILED ($NProblems breaks found)\n");
    }
}

sub writeFile($$)
{
    my ($Path, $Content) = @_;
    return if(not $Path);
    if(my $Dir = get_dirname($Path)) {
        mkpath($Dir);
    }
    open (FILE, ">".$Path) || die ("can't open file \'$Path\': $!\n");
    print FILE $Content;
    close(FILE);
}

sub readFile($)
{
    my $Path = $_[0];
    return "" if(not $Path or not -f $Path);
    open (FILE, $Path);
    my $Content = join("", <FILE>);
    close(FILE);
    $Content=~s/\r//g;
    return $Content;
}

sub appendFile($$)
{
    my ($Path, $Content) = @_;
    return if(not $Path);
    if(my $Dir = get_dirname($Path)) {
        mkpath($Dir);
    }
    open(FILE, ">>".$Path) || die ("can't open file \'$Path\': $!\n");
    print FILE $Content;
    close(FILE);
}

sub get_Report_Header($)
{
    my $Level = $_[0];
    my $Report_Header = "<h1>";
    if($Level eq "Source") {
        $Report_Header .= "Source compatibility";
    }
    elsif($Level eq "Binary") {
        $Report_Header .= "Binary compatibility";
    }
    else {
        $Report_Header .= "API compatibility";
    }
    $Report_Header .= " report for the <span style='color:Blue;'>$TargetTitle</span> library between <span style='color:Red;'>".$Descriptor{1}{"Version"}."</span> and <span style='color:Red;'>".$Descriptor{2}{"Version"}."</span> versions";
    if($ClientPath) {
        $Report_Header .= " (concerning portability of the client: <span style='color:Blue;'>".get_filename($ClientPath)."</span>)";
    }
    $Report_Header .= "</h1>\n";
    return $Report_Header;
}

sub get_SourceInfo()
{
    my $CheckedArchives = "<a name='Checked_Archives'></a>";
    if($OldStyle) {
        $CheckedArchives .= "<h2>Java ARchives (".keys(%{$LibArchives{1}}).")</h2>";
    }
    else {
        $CheckedArchives .= "<h2>Java ARchives <span class='gray'>&nbsp;".keys(%{$LibArchives{1}})."&nbsp;</span></h2>";
    }
    $CheckedArchives .= "\n<hr/><div class='jar_list'>\n";
    foreach my $ArchivePath (sort {lc($a) cmp lc($b)}  keys(%{$LibArchives{1}})) {
        $CheckedArchives .= get_filename($ArchivePath)."<br/>\n";
    }
    $CheckedArchives .= "</div><br/>$TOP_REF<br/>\n";
    return $CheckedArchives;
}

sub get_TypeProblems_Count($$)
{
    my ($TargetSeverity, $Level) = @_;
    my $Type_Problems_Count = 0;
    
    foreach my $Type_Name (sort keys(%{$TypeChanges{$Level}}))
    {
        my %Kinds_Target = ();
        foreach my $Kind (sort keys(%{$TypeChanges{$Level}{$Type_Name}}))
        {
            if($TypeProblems_Kind{$Level}{$Kind} ne $TargetSeverity) {
                next;
            }
            foreach my $Location (sort keys(%{$TypeChanges{$Level}{$Type_Name}{$Kind}}))
            {
                my $Target = $TypeChanges{$Level}{$Type_Name}{$Kind}{$Location}{"Target"};
                
                if($Kinds_Target{$Kind}{$Target}) {
                    next;
                }
                
                $Kinds_Target{$Kind}{$Target} = 1;
                $Type_Problems_Count += 1;
            }
        }
    }
    
    return $Type_Problems_Count;
}

sub show_number($)
{
    if($_[0])
    {
        my $Num = cut_off_number($_[0], 2, 0);
        if($Num eq "0")
        {
            foreach my $P (3 .. 7)
            {
                $Num = cut_off_number($_[0], $P, 1);
                if($Num ne "0") {
                    last;
                }
            }
        }
        if($Num eq "0") {
            $Num = $_[0];
        }
        return $Num;
    }
    return $_[0];
}

sub cut_off_number($$$)
{
    my ($num, $digs_to_cut, $z) = @_;
    if($num!~/\./)
    {
        $num .= ".";
        foreach (1 .. $digs_to_cut-1) {
            $num .= "0";
        }
    }
    elsif($num=~/\.(.+)\Z/ and length($1)<$digs_to_cut-1)
    {
        foreach (1 .. $digs_to_cut - 1 - length($1)) {
            $num .= "0";
        }
    }
    elsif($num=~/\d+\.(\d){$digs_to_cut,}/) {
      $num=sprintf("%.".($digs_to_cut-1)."f", $num);
    }
    $num=~s/\.[0]+\Z//g;
    if($z) {
        $num=~s/(\.[1-9]+)[0]+\Z/$1/g;
    }
    return $num;
}

sub get_Summary($)
{
    my $Level = $_[0];
    my ($Added, $Removed, $M_Problems_High, $M_Problems_Medium, $M_Problems_Low,
    $T_Problems_High, $T_Problems_Medium, $T_Problems_Low, $M_Other, $T_Other) = (0,0,0,0,0,0,0,0,0,0);
    
    %{$RESULT{$Level}} = (
        "Problems"=>0,
        "Warnings"=>0,
        "Affected"=>0 );
    
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if(my $Severity = $MethodProblems_Kind{$Level}{$Kind})
            {
                foreach my $Location (sort keys(%{$CompatProblems{$Method}{$Kind}}))
                {
                    if($Kind eq "Added_Method")
                    {
                        if($Level eq "Source")
                        {
                            if($ChangedReturnFromVoid{$Method}) {
                                next;
                            }
                        }
                        $Added+=1;
                    }
                    elsif($Kind eq "Removed_Method")
                    {
                        if($Level eq "Source")
                        {
                            if($ChangedReturnFromVoid{$Method}) {
                                next;
                            }
                        }
                        $Removed+=1;
                        $TotalAffected{$Level}{$Method} = $Severity;
                    }
                    else
                    {
                        if($Severity eq "Safe") {
                            $M_Other += 1;
                        }
                        elsif($Severity eq "High") {
                            $M_Problems_High+=1;
                        }
                        elsif($Severity eq "Medium") {
                            $M_Problems_Medium+=1;
                        }
                        elsif($Severity eq "Low") {
                            $M_Problems_Low+=1;
                        }
                        if(($Severity ne "Low" or $StrictCompat)
                        and $Severity ne "Safe") {
                            $TotalAffected{$Level}{$Method} = $Severity;
                        }
                    }
                }
            }
        }
    }
    
    my %MethodTypeIndex = ();
    
    foreach my $Method (sort keys(%CompatProblems))
    {
        my @Kinds = sort keys(%{$CompatProblems{$Method}});
        foreach my $Kind (@Kinds)
        {
            if(my $Severity = $TypeProblems_Kind{$Level}{$Kind})
            {
                my @Locs = sort {length($a)<=>length($b)} sort keys(%{$CompatProblems{$Method}{$Kind}});
                foreach my $Location (@Locs)
                {
                    my $Type_Name = $CompatProblems{$Method}{$Kind}{$Location}{"Type_Name"};
                    my $Target = $CompatProblems{$Method}{$Kind}{$Location}{"Target"};
                    
                    if(defined $MethodTypeIndex{$Method}{$Type_Name}{$Kind}{$Target})
                    { # one location for one type and target
                        next;
                    }
                    $MethodTypeIndex{$Method}{$Type_Name}{$Kind}{$Target} = 1;
                    $TypeChanges{$Level}{$Type_Name}{$Kind}{$Location} = $CompatProblems{$Method}{$Kind}{$Location};
                    
                    if(($Severity ne "Low" or $StrictCompat)
                    and $Severity ne "Safe")
                    {
                        if(my $Sev = $TotalAffected{$Level}{$Method})
                        {
                            if($Severity_Val{$Severity}>$Severity_Val{$Sev}) {
                                $TotalAffected{$Level}{$Method} = $Severity;
                            }
                        }
                        else {
                            $TotalAffected{$Level}{$Method} = $Severity;
                        }
                    }
                }
            }
        }
    }
    
    %MethodTypeIndex = (); # clear memory
    
    
    $T_Problems_High = get_TypeProblems_Count("High", $Level);
    $T_Problems_Medium = get_TypeProblems_Count("Medium", $Level);
    $T_Problems_Low = get_TypeProblems_Count("Low", $Level);
    $T_Other = get_TypeProblems_Count("Safe", $Level);
    
    my $SCount = keys(%CheckedMethods)-$Added;
    if($SCount)
    {
        my %Weight = (
            "High" => 100,
            "Medium" => 50,
            "Low" => 25
        );
        foreach (keys(%{$TotalAffected{$Level}})) {
            $RESULT{$Level}{"Affected"}+=$Weight{$TotalAffected{$Level}{$_}};
        }
        $RESULT{$Level}{"Affected"} = $RESULT{$Level}{"Affected"}/$SCount;
    }
    else {
        $RESULT{$Level}{"Affected"} = 0;
    }
    $RESULT{$Level}{"Affected"} = show_number($RESULT{$Level}{"Affected"});
    if($RESULT{$Level}{"Affected"}>=100) {
        $RESULT{$Level}{"Affected"} = 100;
    }
    
    my ($TestInfo, $TestResults, $Problem_Summary) = ();
    
    # test info
    $TestInfo .= "<h2>Test Info</h2><hr/>\n";
    $TestInfo .= "<table class='summary'>\n";
    $TestInfo .= "<tr><th>Library Name</th><td>$TargetTitle</td></tr>\n";
    $TestInfo .= "<tr><th>Version #1</th><td>".$Descriptor{1}{"Version"}."</td></tr>\n";
    $TestInfo .= "<tr><th>Version #2</th><td>".$Descriptor{2}{"Version"}."</td></tr>\n";
    
    if($JoinReport)
    {
        if($Level eq "Binary") {
            $TestInfo .= "<tr><th>Subject</th><td width='150px'>Binary Compatibility</td></tr>\n"; # Run-time
        }
        if($Level eq "Source") {
            $TestInfo .= "<tr><th>Subject</th><td width='150px'>Source Compatibility</td></tr>\n"; # Build-time
        }
    }
    $TestInfo .= "</table>\n";
    
    # test results
    $TestResults .= "<h2>Test Results</h2><hr/>";
    $TestResults .= "<table class='summary'>";
    
    my $Checked_Archives_Link = "0";
    $Checked_Archives_Link = "<a href='#Checked_Archives' style='color:Blue;'>".keys(%{$LibArchives{1}})."</a>" if(keys(%{$LibArchives{1}})>0);
    
    $TestResults .= "<tr><th>Total JARs</th><td>$Checked_Archives_Link</td></tr>\n";
    $TestResults .= "<tr><th>Total Methods / Classes</th><td>".keys(%CheckedMethods)." / ".keys(%CheckedTypes)."</td></tr>\n";
    
    $RESULT{$Level}{"Problems"} += $Removed+$M_Problems_High+$T_Problems_High+$T_Problems_Medium+$M_Problems_Medium;
    if($StrictCompat) {
        $RESULT{$Level}{"Problems"}+=$T_Problems_Low+$M_Problems_Low;
    }
    else {
        $RESULT{$Level}{"Warnings"}+=$T_Problems_Low+$M_Problems_Low;
    }
    
    my $META_DATA = "kind:".lc($Level).";";
    $META_DATA .= $RESULT{$Level}{"Problems"}?"verdict:incompatible;":"verdict:compatible;";
    $TestResults .= "<tr><th>Compatibility</th>\n";
    
    my $BC_Rate = show_number(100 - $RESULT{$Level}{"Affected"});
    
    if($RESULT{$Level}{"Problems"})
    {
        my $Cl = "incompatible";
        if($BC_Rate>=90) {
            $Cl = "warning";
        }
        elsif($BC_Rate>=80) {
            $Cl = "almost_compatible";
        }
        
        $TestResults .= "<td class=\'$Cl\'>".$BC_Rate."%</td>\n";
    }
    else
    {
        $TestResults .= "<td class=\'compatible\'>100%</td>\n";
    }
    
    $TestResults .= "</tr>\n";
    $TestResults .= "</table>\n";
    
    $META_DATA .= "affected:".$RESULT{$Level}{"Affected"}.";";# in percents
    
    # Problem Summary
    $Problem_Summary .= "<h2>Problem Summary</h2><hr/>";
    $Problem_Summary .= "<table class='summary'>";
    $Problem_Summary .= "<tr><th></th><th style='text-align:center;'>Severity</th><th style='text-align:center;'>Count</th></tr>";
    
    my $Added_Link = "0";
    if($Added>0)
    {
        if($ShortMode) {
            $Added_Link = $Added;
        }
        else
        {
            if($JoinReport) {
                $Added_Link = "<a href='#".$Level."_Added' style='color:Blue;'>$Added</a>";
            }
            else {
                $Added_Link = "<a href='#Added' style='color:Blue;'>$Added</a>";
            }
        }
    }
    $META_DATA .= "added:$Added;";
    $Problem_Summary .= "<tr><th>Added Methods</th><td>-</td><td".getStyle("M", "Added", $Added).">$Added_Link</td></tr>";
    
    my $Removed_Link = "0";
    if($Removed>0)
    {
        if($ShortMode) {
            $Removed_Link = $Removed;
        }
        else
        {
            if($JoinReport) {
                $Removed_Link = "<a href='#".$Level."_Removed' style='color:Blue;'>$Removed</a>"
            }
            else {
                $Removed_Link = "<a href='#Removed' style='color:Blue;'>$Removed</a>"
            }
        }
    }
    $META_DATA .= "removed:$Removed;";
    $Problem_Summary .= "<tr><th>Removed Methods</th>";
    $Problem_Summary .= "<td>High</td><td".getStyle("M", "Removed", $Removed).">$Removed_Link</td></tr>";
    
    my $TH_Link = "0";
    $TH_Link = "<a href='#".get_Anchor("Type", $Level, "High")."' style='color:Blue;'>$T_Problems_High</a>" if($T_Problems_High>0);
    $META_DATA .= "type_problems_high:$T_Problems_High;";
    $Problem_Summary .= "<tr><th rowspan='3'>Problems with<br/>Data Types</th>";
    $Problem_Summary .= "<td>High</td><td".getStyle("T", "High", $T_Problems_High).">$TH_Link</td></tr>";
    
    my $TM_Link = "0";
    $TM_Link = "<a href='#".get_Anchor("Type", $Level, "Medium")."' style='color:Blue;'>$T_Problems_Medium</a>" if($T_Problems_Medium>0);
    $META_DATA .= "type_problems_medium:$T_Problems_Medium;";
    $Problem_Summary .= "<tr><td>Medium</td><td".getStyle("T", "Medium", $T_Problems_Medium).">$TM_Link</td></tr>";
    
    my $TL_Link = "0";
    $TL_Link = "<a href='#".get_Anchor("Type", $Level, "Low")."' style='color:Blue;'>$T_Problems_Low</a>" if($T_Problems_Low>0);
    $META_DATA .= "type_problems_low:$T_Problems_Low;";
    $Problem_Summary .= "<tr><td>Low</td><td".getStyle("T", "Low", $T_Problems_Low).">$TL_Link</td></tr>";
    
    my $MH_Link = "0";
    $MH_Link = "<a href='#".get_Anchor("Method", $Level, "High")."' style='color:Blue;'>$M_Problems_High</a>" if($M_Problems_High>0);
    $META_DATA .= "method_problems_high:$M_Problems_High;";
    $Problem_Summary .= "<tr><th rowspan='3'>Problems with<br/>Methods</th>";
    $Problem_Summary .= "<td>High</td><td".getStyle("M", "High", $M_Problems_High).">$MH_Link</td></tr>";
    
    my $MM_Link = "0";
    $MM_Link = "<a href='#".get_Anchor("Method", $Level, "Medium")."' style='color:Blue;'>$M_Problems_Medium</a>" if($M_Problems_Medium>0);
    $META_DATA .= "method_problems_medium:$M_Problems_Medium;";
    $Problem_Summary .= "<tr><td>Medium</td><td".getStyle("M", "Medium", $M_Problems_Medium).">$MM_Link</td></tr>";
    
    my $ML_Link = "0";
    $ML_Link = "<a href='#".get_Anchor("Method", $Level, "Low")."' style='color:Blue;'>$M_Problems_Low</a>" if($M_Problems_Low>0);
    $META_DATA .= "method_problems_low:$M_Problems_Low;";
    $Problem_Summary .= "<tr><td>Low</td><td".getStyle("M", "Low", $M_Problems_Low).">$ML_Link</td></tr>";
    
    # Safe Changes
    if($T_Other)
    {
        my $TS_Link = "<a href='#".get_Anchor("Type", $Level, "Safe")."' style='color:Blue;'>$T_Other</a>";
        $Problem_Summary .= "<tr><th>Other Changes<br/>in Data Types</th><td>-</td><td".getStyle("T", "Safe", $T_Other).">$TS_Link</td></tr>\n";
    }
    
    if($M_Other)
    {
        my $MS_Link = "<a href='#".get_Anchor("Method", $Level, "Safe")."' style='color:Blue;'>$M_Other</a>";
        $Problem_Summary .= "<tr><th>Other Changes<br/>in Methods</th><td>-</td><td".getStyle("M", "Safe", $M_Other).">$MS_Link</td></tr>\n";
    }
    $META_DATA .= "checked_methods:".keys(%CheckedMethods).";";
    $META_DATA .= "checked_types:".keys(%CheckedTypes).";";
    $META_DATA .= "tool_version:$TOOL_VERSION";
    $Problem_Summary .= "</table>\n";
    return ($TestInfo.$TestResults.$Problem_Summary, $META_DATA);
}

sub getStyle($$$)
{
    my ($Subj, $Act, $Num) = @_;
    my %Style = (
        "Added"=>"new",
        "Removed"=>"failed",
        "Safe"=>"passed",
        "Low"=>"warning",
        "Medium"=>"failed",
        "High"=>"failed"
    );
    
    if($Num>0) {
        return " class='".$Style{$Act}."'";
    }
    
    return "";
}

sub get_Anchor($$$)
{
    my ($Kind, $Level, $Severity) = @_;
    if($JoinReport)
    {
        if($Severity eq "Safe") {
            return "Other_".$Level."_Changes_In_".$Kind."s";
        }
        else {
            return $Kind."_".$Level."_Problems_".$Severity;
        }
    }
    else
    {
        if($Severity eq "Safe") {
            return "Other_Changes_In_".$Kind."s";
        }
        else {
            return $Kind."_Problems_".$Severity;
        }
    }
}

sub get_Report_Added($)
{
    if($ShortMode) {
        return "";
    }
    
    my $Level = $_[0];
    my ($ADDED_METHODS, %MethodAddedInArchiveClass);
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($Kind eq "Added_Method")
            {
                my $ArchiveName = $MethodInfo{2}{$Method}{"Archive"};
                my $ClassName = get_ShortName($MethodInfo{2}{$Method}{"Class"}, 2);
                if($Level eq "Source")
                {
                    if($ChangedReturnFromVoid{$Method}) {
                        next;
                    }
                }
                $MethodAddedInArchiveClass{$ArchiveName}{$ClassName}{$Method} = 1;
            }
        }
    }
    my $Added_Number = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%MethodAddedInArchiveClass))
    {
        foreach my $ClassName (sort {lc($a) cmp lc($b)} keys(%{$MethodAddedInArchiveClass{$ArchiveName}}))
        {
            my %NameSpace_Method = ();
            foreach my $Method (keys(%{$MethodAddedInArchiveClass{$ArchiveName}{$ClassName}})) {
                $NameSpace_Method{$MethodInfo{2}{$Method}{"Package"}}{$Method} = 1;
            }
            
            my $ShowClass = $ClassName;
            $ShowClass=~s/<.*>//g;
            
            foreach my $NameSpace (sort keys(%NameSpace_Method))
            {
                $ADDED_METHODS .= "<span class='jar'>$ArchiveName</span>, <span class='cname'>".htmlSpecChars($ShowClass).".class</span><br/>\n";
                
                if($NameSpace) {
                    $ADDED_METHODS .= "<span class='pkg_t'>package</span> <span class='pkg'>$NameSpace</span><br/>\n";
                }
                
                if($Compact) {
                    $ADDED_METHODS .= "<div class='symbols'>";
                }
                
                my @SortedMethods = sort {lc($MethodInfo{2}{$a}{"Signature"}) cmp lc($MethodInfo{2}{$b}{"Signature"})} keys(%{$NameSpace_Method{$NameSpace}});
                foreach my $Method (@SortedMethods)
                {
                    $Added_Number += 1;
                    
                    my $Signature = undef;
                    
                    if($Compact) {
                        $Signature = get_Signature($Method, 2, "Full|HTML|Simple");
                    }
                    else {
                        $Signature = highLight_Signature_Italic_Color($Method, 2);
                    }
                    
                    if($NameSpace) {
                        $Signature=~s/(\W|\A)\Q$NameSpace\E\.(\w)/$1$2/g;
                    }
                    
                    if($Compact) {
                        $ADDED_METHODS .= "&nbsp;".$Signature."<br/>\n";
                    }
                    else {
                        $ADDED_METHODS .= insertIDs($ContentSpanStart.$Signature.$ContentSpanEnd."<br/>\n".$ContentDivStart."<span class='mngl'>[mangled: <b>".htmlSpecChars($Method)."</b>]</span><br/><br/>".$ContentDivEnd."\n");
                    }
                }
                
                if($Compact) {
                    $ADDED_METHODS .= "</div>";
                }
                
                $ADDED_METHODS .= "<br/>\n";
            }
            
        }
    }
    if($ADDED_METHODS)
    {
        my $Anchor = "<a name='Added'></a>";
        if($JoinReport) {
            $Anchor = "<a name='".$Level."_Added'></a>";
        }
        if($OldStyle) {
            $ADDED_METHODS = "<h2>Added Methods ($Added_Number)</h2><hr/>\n".$ADDED_METHODS;
        }
        else {
            $ADDED_METHODS = "<h2>Added Methods <span".getStyle("M", "Added", $Added_Number).">&nbsp;$Added_Number&nbsp;</span></h2><hr/>\n".$ADDED_METHODS;
        }
        $ADDED_METHODS = $Anchor.$ADDED_METHODS.$TOP_REF."<br/>\n";
    }
    return $ADDED_METHODS;
}

sub get_Report_Removed($)
{
    if($ShortMode) {
        return "";
    }
    
    my $Level = $_[0];
    my ($REMOVED_METHODS, %MethodRemovedFromArchiveClass);
    foreach my $Method (sort keys(%CompatProblems))
    {
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($Kind eq "Removed_Method")
            {
                if($Level eq "Source")
                {
                    if($ChangedReturnFromVoid{$Method}) {
                        next;
                    }
                }
                my $ArchiveName = $MethodInfo{1}{$Method}{"Archive"};
                my $ClassName = get_ShortName($MethodInfo{1}{$Method}{"Class"}, 1);
                $MethodRemovedFromArchiveClass{$ArchiveName}{$ClassName}{$Method} = 1;
            }
        }
    }
    my $Removed_Number = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%MethodRemovedFromArchiveClass))
    {
        foreach my $ClassName (sort {lc($a) cmp lc($b)} keys(%{$MethodRemovedFromArchiveClass{$ArchiveName}}))
        {
            my %NameSpace_Method = ();
            foreach my $Method (keys(%{$MethodRemovedFromArchiveClass{$ArchiveName}{$ClassName}}))
            {
                $NameSpace_Method{$MethodInfo{1}{$Method}{"Package"}}{$Method} = 1;
            }
            
            my $ShowClass = $ClassName;
            $ShowClass=~s/<.*>//g;
            
            foreach my $NameSpace (sort keys(%NameSpace_Method))
            {
                $REMOVED_METHODS .= "<span class='jar'>$ArchiveName</span>, <span class='cname'>".htmlSpecChars($ShowClass).".class</span><br/>\n";
                
                if($NameSpace) {
                    $REMOVED_METHODS .= "<span class='pkg_t'>package</span> <span class='pkg'>$NameSpace</span><br/>\n";
                }
                
                if($Compact) {
                    $REMOVED_METHODS .= "<div class='symbols'>";
                }
                
                my @SortedMethods = sort {lc($MethodInfo{1}{$a}{"Signature"}) cmp lc($MethodInfo{1}{$b}{"Signature"})} keys(%{$NameSpace_Method{$NameSpace}});
                foreach my $Method (@SortedMethods)
                {
                    $Removed_Number += 1;
                    
                    my $Signature = undef;
                    
                    if($Compact) {
                        $Signature = get_Signature($Method, 1, "Full|HTML|Simple");
                    }
                    else {
                        $Signature = highLight_Signature_Italic_Color($Method, 1);
                    }
                    
                    if($NameSpace) {
                        $Signature=~s/(\W|\A)\Q$NameSpace\E\.(\w)/$1$2/g;
                    }
                    
                    if($Compact) {
                        $REMOVED_METHODS .= "&nbsp;".$Signature."<br/>\n";
                    }
                    else {
                        $REMOVED_METHODS .= insertIDs($ContentSpanStart.$Signature.$ContentSpanEnd."<br/>\n".$ContentDivStart."<span class='mngl'>[mangled: <b>".htmlSpecChars($Method)."</b>]</span><br/><br/>".$ContentDivEnd."\n");
                    }
                }
                
                if($Compact) {
                    $REMOVED_METHODS .= "</div>";
                }
                
                $REMOVED_METHODS .= "<br/>\n";
            }
        }
    }
    if($REMOVED_METHODS)
    {
        my $Anchor = "<a name='Removed'></a><a name='Withdrawn'></a>";
        if($JoinReport) {
            $Anchor = "<a name='".$Level."_Removed'></a><a name='".$Level."_Withdrawn'></a>";
        }
        if($OldStyle) {
            $REMOVED_METHODS = "<h2>Removed Methods ($Removed_Number)</h2><hr/>\n".$REMOVED_METHODS;
        }
        else {
            $REMOVED_METHODS = "<h2>Removed Methods <span".getStyle("M", "Removed", $Removed_Number).">&nbsp;$Removed_Number&nbsp;</span></h2><hr/>\n".$REMOVED_METHODS;
        }
        $REMOVED_METHODS = $Anchor.$REMOVED_METHODS.$TOP_REF."<br/>\n";
    }
    return $REMOVED_METHODS;
}

sub get_Report_MethodProblems($$)
{
    my ($TargetSeverity, $Level) = @_;
    my $METHOD_PROBLEMS = "";
    my (%ReportMap, %MethodChanges) = ();
    
    foreach my $Method (sort keys(%CompatProblems))
    {
        my $ArchiveName = $MethodInfo{1}{$Method}{"Archive"};
        my $ClassName = get_ShortName($MethodInfo{1}{$Method}{"Class"}, 1);
        
        foreach my $Kind (sort keys(%{$CompatProblems{$Method}}))
        {
            if($Kind eq "Added_Method"
            or $Kind eq "Removed_Method") {
                next;
            }
            
            if(my $Severity = $MethodProblems_Kind{$Level}{$Kind})
            {
                if($Severity ne $TargetSeverity) {
                    next;
                }
                
                $MethodChanges{$Method}{$Kind} = $CompatProblems{$Method}{$Kind};
                $ReportMap{$ArchiveName}{$ClassName}{$Method} = 1;
            }
        }
    }
    my $ProblemsNum = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%ReportMap))
    {
        foreach my $ClassName (sort {lc($a) cmp lc($b)} keys(%{$ReportMap{$ArchiveName}}))
        {
            my %NameSpace_Method = ();
            foreach my $Method (keys(%{$ReportMap{$ArchiveName}{$ClassName}})) {
                $NameSpace_Method{$MethodInfo{1}{$Method}{"Package"}}{$Method} = 1;
            }
            
            my $ShowClass = $ClassName;
            $ShowClass=~s/<.*>//g;
            
            foreach my $NameSpace (sort keys(%NameSpace_Method))
            {
                $METHOD_PROBLEMS .= "<span class='jar'>$ArchiveName</span>, <span class='cname'>".htmlSpecChars($ShowClass).".class</span><br/>\n";
                if($NameSpace) {
                    $METHOD_PROBLEMS .= "<span class='pkg_t'>package</span> <span class='pkg'>$NameSpace</span><br/>\n";
                }
                
                my @SortedMethods = sort {lc($MethodInfo{1}{$a}{"Signature"}) cmp lc($MethodInfo{1}{$b}{"Signature"})} keys(%{$NameSpace_Method{$NameSpace}});
                foreach my $Method (@SortedMethods)
                {
                    my $ShortSignature = get_Signature($Method, 1, "Short");
                    my $ClassName_Full = get_TypeName($MethodInfo{1}{$Method}{"Class"}, 1);
                    my $METHOD_REPORT = "";
                    my $ProblemNum = 1;
                    foreach my $Kind (sort keys(%{$MethodChanges{$Method}}))
                    {
                        foreach my $Location (sort keys(%{$MethodChanges{$Method}{$Kind}}))
                        {
                            my %Problems = %{$MethodChanges{$Method}{$Kind}{$Location}};
                            
                            my $Target = $Problems{"Target"};
                            
                            my ($Change, $Effect) = ("", "");
                            my $Old_Value = htmlSpecChars($Problems{"Old_Value"});
                            my $New_Value = htmlSpecChars($Problems{"New_Value"});
                            
                            if($Kind eq "Method_Became_Static")
                            {
                                $Change = "Method became <b>static</b>.\n";
                                $Effect = "A client program may be interrupted by <b>NoSuchMethodError</b> exception.";
                            }
                            elsif($Kind eq "Method_Became_NonStatic")
                            {
                                $Change = "Method became <b>non-static</b>.\n";
                                if($Level eq "Binary") {
                                    $Effect = "A client program may be interrupted by <b>NoSuchMethodError</b> exception.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: non-static method ".htmlSpecChars($ShortSignature)." cannot be referenced from a static context.";
                                }
                            }
                            elsif($Kind eq "Changed_Method_Return_From_Void")
                            {
                                $Change = "Return value type has been changed from <b>void</b> to <b>".htmlSpecChars($New_Value)."</b>.\n";
                                $Effect = "This method has been removed because the return type is part of the method signature.";
                            }
                            elsif($Kind eq "Static_Method_Became_Final")
                            {# Source Only
                                $Change = "Method became <b>final</b>.\n";
                                $Effect = "Recompilation of a client program may be terminated with the message: ".htmlSpecChars($ShortSignature)." in client class C cannot override ".htmlSpecChars($ShortSignature)." in ".htmlSpecChars($ClassName_Full)."; overridden method is final.";
                            }
                            elsif($Kind eq "NonStatic_Method_Became_Final")
                            {
                                $Change = "Method became <b>final</b>.\n";
                                if($Level eq "Binary") {
                                    $Effect = "A client program trying to reimplement this method may be interrupted by <b>VerifyError</b> exception.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: ".htmlSpecChars($ShortSignature)." in client class C cannot override ".htmlSpecChars($ShortSignature)." in ".htmlSpecChars($ClassName_Full)."; overridden method is final.";
                                }
                            }
                            elsif($Kind eq "Method_Became_Abstract")
                            {
                                $Change = "Method became <b>abstract</b>.\n";
                                if($Level eq "Binary") {
                                    $Effect = "A client program trying to create an instance of the method's class may be interrupted by <b>InstantiationError</b> exception.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: A client class C is not abstract and does not override abstract method ".htmlSpecChars($ShortSignature)." in ".htmlSpecChars($ClassName_Full).".";
                                }
                            }
                            elsif($Kind eq "Method_Became_NonAbstract")
                            {
                                $Change = "Method became <b>non-abstract</b>.\n";
                                if($Level eq "Binary") {
                                    $Effect = "A client program may change behavior.";
                                }
                                else {
                                    $Effect = "No effect.";
                                }
                            }
                            elsif($Kind eq "Method_Became_Default")
                            {
                                $Change = "Method became <b>default</b>.\n";
                                if($Level eq "Binary") {
                                    $Effect = "No effect.";
                                }
                                else {
                                    $Effect = "No effect.";
                                }
                            }
                            elsif($Kind eq "Method_Became_NonDefault")
                            {
                                $Change = "Method became <b>non-default</b>.\n";
                                if($Level eq "Binary") {
                                    $Effect = "A client program trying to create an instance of a class may be interrupted by <b>AbstractMethodError</b> exception.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: A client class C is not abstract and does not override abstract method ".htmlSpecChars($ShortSignature)." in ".htmlSpecChars($ClassName_Full).".";
                                }
                            }
                            elsif($Kind eq "Method_Became_Synchronized")
                            {
                                $Change = "Method became <b>synchronized</b>.\n";
                                $Effect = "A multi-threaded client program may change behavior.";
                            }
                            elsif($Kind eq "Method_Became_NonSynchronized")
                            {
                                $Change = "Method became <b>non-synchronized</b>.\n";
                                $Effect = "A multi-threaded client program may change behavior.";
                            }
                            elsif($Kind eq "Changed_Method_Access")
                            {
                                $Change = "Access level has been changed from <span class='nowrap'><b>".htmlSpecChars($Old_Value)."</b></span> to <span class='nowrap'><b>".htmlSpecChars($New_Value)."</b></span>.";
                                if($Level eq "Binary") {
                                    $Effect = "A client program may be interrupted by <b>IllegalAccessError</b> exception.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: ".htmlSpecChars($ShortSignature)." has $New_Value access in ".htmlSpecChars($ClassName_Full).".";
                                }
                            }
                            elsif($Kind eq "Abstract_Method_Added_Checked_Exception")
                            {# Source Only
                                $Change = "Added <b>".htmlSpecChars($Target)."</b> exception thrown.\n";
                                $Effect = "Recompilation of a client program may be terminated with the message: unreported exception ".htmlSpecChars($Target)." must be caught or declared to be thrown.";
                            }
                            elsif($Kind eq "NonAbstract_Method_Added_Checked_Exception")
                            {
                                $Change = "Added <b>".htmlSpecChars($Target)."</b> exception thrown.\n";
                                if($Level eq "Binary") {
                                    $Effect = "A client program may be interrupted by added exception.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: unreported exception ".htmlSpecChars($Target)." must be caught or declared to be thrown.";
                                }
                            }
                            elsif($Kind eq "Abstract_Method_Removed_Checked_Exception")
                            {# Source Only
                                $Change = "Removed <b>".htmlSpecChars($Target)."</b> exception thrown.\n";
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot override ".htmlSpecChars($ShortSignature)." in ".htmlSpecChars($ClassName_Full)."; overridden method does not throw ".htmlSpecChars($Target).".";
                            }
                            elsif($Kind eq "NonAbstract_Method_Removed_Checked_Exception")
                            {
                                $Change = "Removed <b>".htmlSpecChars($Target)."</b> exception thrown.\n";
                                if($Level eq "Binary") {
                                    $Effect = "A client program may change behavior because the removed exception will not be thrown any more and client will not catch and handle it.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: cannot override ".htmlSpecChars($ShortSignature)." in ".htmlSpecChars($ClassName_Full)."; overridden method does not throw ".htmlSpecChars($Target).".";
                                }
                            }
                            elsif($Kind eq "Added_Unchecked_Exception")
                            {# Binary Only
                                $Change = "Added <b>".htmlSpecChars($Target)."</b> exception thrown.\n";
                                $Effect = "A client program may be interrupted by added exception.";
                            }
                            elsif($Kind eq "Removed_Unchecked_Exception")
                            {# Binary Only
                                $Change = "Removed <b>".htmlSpecChars($Target)."</b> exception thrown.\n";
                                $Effect = "A client program may change behavior because the removed exception will not be thrown any more and client will not catch and handle it.";
                            }
                            if($Change)
                            {
                                $METHOD_REPORT .= "<tr><th>$ProblemNum</th><td>".$Change."</td><td>".$Effect."</td></tr>\n";
                                $ProblemNum += 1;
                                $ProblemsNum += 1;
                            }
                        }
                    }
                    $ProblemNum -= 1;
                    if($METHOD_REPORT)
                    {
                        my $ShowMethod = highLight_Signature_Italic_Color($Method, 1);
                        if($NameSpace)
                        {
                            $METHOD_REPORT = cut_Namespace($METHOD_REPORT, $NameSpace);
                            $ShowMethod = cut_Namespace($ShowMethod, $NameSpace);
                        }
                        
                        $METHOD_PROBLEMS .= $ContentSpanStart."<span class='ext'>[+]</span> ".$ShowMethod;
                        if($OldStyle) {
                            $METHOD_PROBLEMS .= " ($ProblemNum)";
                        }
                        else {
                            $METHOD_PROBLEMS .= " <span".getStyle("M", $TargetSeverity, $ProblemNum).">&nbsp;$ProblemNum&nbsp;</span>";
                        }
                        $METHOD_PROBLEMS .= $ContentSpanEnd."<br/>\n";
                        $METHOD_PROBLEMS .= $ContentDivStart;
                        
                        if(not $Compact) {
                            $METHOD_PROBLEMS .= "<span class='mngl'>&#160;&#160;&#160;[mangled: <b>".htmlSpecChars($Method)."</b>]</span><br/>\n";
                        }
                        
                        $METHOD_PROBLEMS .= "<table class='ptable'><tr><th width='2%'></th><th width='47%'>Change</th><th>Effect</th></tr>$METHOD_REPORT</table><br/>$ContentDivEnd\n";
                        
                    }
                }
                
                $METHOD_PROBLEMS .= "<br/>";
            }
        }
    }
    if($METHOD_PROBLEMS)
    {
        $METHOD_PROBLEMS = insertIDs($METHOD_PROBLEMS);
        
        my $Title = "Problems with Methods, $TargetSeverity Severity";
        if($TargetSeverity eq "Safe")
        { # Safe Changes
            $Title = "Other Changes in Methods";
        }
        if($OldStyle) {
            $METHOD_PROBLEMS = "<h2>$Title ($ProblemsNum)</h2><hr/>\n".$METHOD_PROBLEMS;
        }
        else {
            $METHOD_PROBLEMS = "<h2>$Title <span".getStyle("M", $TargetSeverity, $ProblemsNum).">&nbsp;$ProblemsNum&nbsp;</span></h2><hr/>\n".$METHOD_PROBLEMS;
        }
        $METHOD_PROBLEMS = "<a name='".get_Anchor("Method", $Level, $TargetSeverity)."'></a>\n".$METHOD_PROBLEMS;
        $METHOD_PROBLEMS .= $TOP_REF."<br/>\n";
    }
    return $METHOD_PROBLEMS;
}

sub get_Report_TypeProblems($$)
{
    my ($TargetSeverity, $Level) = @_;
    my $TYPE_PROBLEMS = "";
    my %ReportMap = ();
    my %TypeChanges_Sev = ();
    
    foreach my $TypeName (keys(%{$TypeChanges{$Level}}))
    {
        my $ArchiveName = $TypeInfo{1}{$TName_Tid{1}{$TypeName}}{"Archive"};
        
        foreach my $Kind (keys(%{$TypeChanges{$Level}{$TypeName}}))
        {
            my $Severity = $TypeProblems_Kind{$Level}{$Kind};
            
            if($Severity ne $TargetSeverity) {
                next;
            }
            
            foreach my $Location (keys(%{$TypeChanges{$Level}{$TypeName}{$Kind}}))
            {
                $ReportMap{$ArchiveName}{$TypeName} = 1;
                $TypeChanges_Sev{$TypeName}{$Kind}{$Location} = $TypeChanges{$Level}{$TypeName}{$Kind}{$Location};
            }
        }
    }
    
    my $ProblemsNum = 0;
    foreach my $ArchiveName (sort {lc($a) cmp lc($b)} keys(%ReportMap))
    {
        my ($HEADER_REPORT, %NameSpace_Type) = ();
        foreach my $TypeName (keys(%{$ReportMap{$ArchiveName}}))
        {
            $NameSpace_Type{$TypeInfo{1}{$TName_Tid{1}{$TypeName}}{"Package"}}{$TypeName} = 1;
        }
        foreach my $NameSpace (sort keys(%NameSpace_Type))
        {
            $TYPE_PROBLEMS .= "<span class='jar'>$ArchiveName</span><br/>\n";
            if($NameSpace) {
                $TYPE_PROBLEMS .= "<span class='pkg_t'>package</span> <span class='pkg'>".$NameSpace."</span><br/>\n";
            }
            
            my @SortedTypes = sort {lc($a) cmp lc($b)} keys(%{$NameSpace_Type{$NameSpace}});
            foreach my $TypeName (@SortedTypes)
            {
                my $TypeId = $TName_Tid{1}{$TypeName};
                my $ProblemNum = 1;
                my $TYPE_REPORT = "";
                my (%Kinds_Locations, %Kinds_Target) = ();
                foreach my $Kind (sort keys(%{$TypeChanges_Sev{$TypeName}}))
                {
                    foreach my $Location (sort keys(%{$TypeChanges_Sev{$TypeName}{$Kind}}))
                    {
                        $Kinds_Locations{$Kind}{$Location} = 1;
                        
                        my $Target = $TypeChanges_Sev{$TypeName}{$Kind}{$Location}{"Target"};
                        if($Kinds_Target{$Kind}{$Target}) {
                            next;
                        }
                        $Kinds_Target{$Kind}{$Target} = 1;
                        
                        my ($Change, $Effect) = ("", "");
                        my %Problems = %{$TypeChanges_Sev{$TypeName}{$Kind}{$Location}};
                        
                        my $Old_Value = $Problems{"Old_Value"};
                        my $New_Value = $Problems{"New_Value"};
                        my $Field_Type = $Problems{"Field_Type"};
                        my $Field_Value = $Problems{"Field_Value"};
                        my $Type_Type = $Problems{"Type_Type"};
                        
                        if($Kind eq "NonAbstract_Class_Added_Abstract_Method")
                        {
                            $Change = "Abstract method ".black_Name($Target, 2)." has been added to this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "This class became <b>abstract</b> and a client program may be interrupted by <b>InstantiationError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method <b>".htmlSpecChars(get_Signature($Target, 2, "Short"))."</b> in <b>".htmlSpecChars(get_TypeName($MethodInfo{2}{$Target}{"Class"}, 2))."</b>.";
                            }
                        }
                        elsif($Kind eq "Abstract_Class_Added_Abstract_Method")
                        {
                            $Change = "Abstract method ".black_Name($Target, 2)." has been added to this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method <b>".htmlSpecChars(get_Signature($Target, 2, "Short"))."</b> in <b>".htmlSpecChars(get_TypeName($MethodInfo{2}{$Target}{"Class"}, 2))."</b>.";
                            }
                        }
                        elsif($Kind eq "Abstract_Class_Added_Abstract_Method_Invoked_By_Others")
                        {
                            $Change = "Abstract method ".black_Name($Target, 2)." has been added to this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>AbstractMethodError</b> exception. Added abstract method is called in 2nd library version by the method ".black_Name($Problems{"InvokedBy"}, 1)." and may not be implemented by old clients.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method <b>".htmlSpecChars(get_Signature($Target, 2, "Short"))."</b> in <b>".htmlSpecChars(get_TypeName($MethodInfo{2}{$Target}{"Class"}, 2))."</b>.";
                            }
                        }
                        elsif($Kind eq "Class_Removed_Abstract_Method"
                        or $Kind eq "Interface_Removed_Abstract_Method")
                        {
                            $Change = "Abstract method ".black_Name($Target, 1)." has been removed from this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>NoSuchMethodError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find method <b>".htmlSpecChars(get_Signature($Target, 1, "Short"))."</b> in $Type_Type <b>".htmlSpecChars(get_TypeName($MethodInfo{1}{$Target}{"Class"}, 1))."</b>.";
                            }
                        }
                        elsif($Kind eq "Interface_Added_Abstract_Method")
                        {
                            $Change = "Abstract method ".black_Name($Target, 2)." has been added to this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method <b>".htmlSpecChars(get_Signature($Target, 2, "Short"))."</b> in <b>".htmlSpecChars(get_TypeName($MethodInfo{2}{$Target}{"Class"}, 2))."</b>.";
                            }
                        }
                        elsif($Kind eq "Interface_Added_Abstract_Method_Invoked_By_Others")
                        {
                            $Change = "Abstract method ".black_Name($Target, 2)." has been added to this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>AbstractMethodError</b> exception. Added abstract method is called in 2nd library version by the method ".black_Name($Problems{"InvokedBy"}, 1)." and may not be implemented by old clients.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method <b>".htmlSpecChars(get_Signature($Target, 2, "Short"))."</b> in <b>".htmlSpecChars(get_TypeName($MethodInfo{2}{$Target}{"Class"}, 2))."</b>.";
                            }
                        }
                        elsif($Kind eq "Class_Method_Became_Abstract")
                        {
                            $Change = "Method ".black_Name($Target, 1)." became <b>abstract</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>InstantiationError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method <b>".htmlSpecChars(get_Signature($Target, 1, "Short"))."</b> in <b>".htmlSpecChars(get_TypeName($MethodInfo{1}{$Target}{"Class"}, 1))."</b>.";
                            }
                        }
                        elsif($Kind eq "Class_Method_Became_NonAbstract")
                        {
                            $Change = "Abstract method ".black_Name($Target, 1)." became <b>non-abstract</b>.";
                            if($Level eq "Binary") {
                                $Effect = "Some methods in this class may change behavior.";
                            }
                            else {
                                $Effect = "No effect.";
                            }
                        }
                        elsif($Kind eq "Interface_Method_Became_NonDefault")
                        {
                            $Change = "Method ".black_Name($Target, 1)." became <b>non-default</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>AbstractMethodError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method <b>".htmlSpecChars(get_Signature($Target, 1, "Short"))."</b> in <b>".htmlSpecChars(get_TypeName($MethodInfo{1}{$Target}{"Class"}, 1))."</b>.";
                            }
                        }
                        elsif($Kind eq "Interface_Method_Became_Default")
                        {
                            $Change = "Method ".black_Name($Target, 1)." became <b>default</b>.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "No effect.";
                            }
                        }
                        elsif($Kind eq "Class_Overridden_Method")
                        {
                            $Change = "Method ".black_Name($Old_Value, 2)." has been overridden by ".black_Name($New_Value, 2);
                            $Effect = "Method ".black_Name($New_Value, 2)." will be called instead of ".black_Name($Old_Value, 2)." in a client program.";
                        }
                        elsif($Kind eq "Class_Method_Moved_Up_Hierarchy")
                        {
                            $Change = "Method ".black_Name($Old_Value, 1)." has been moved up type hierarchy to ".black_Name($New_Value, 2);
                            $Effect = "Method ".black_Name($New_Value, 2)." will be called instead of ".black_Name($Old_Value, 1)." in a client program.";
                        }
                        elsif($Kind eq "Abstract_Class_Added_Super_Interface")
                        {
                            $Change = "Added super-interface <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary")
                            {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method in <b>".htmlSpecChars($Target)."</b>.";
                            }
                        }
                        elsif($Kind eq "Abstract_Class_Added_Super_Interface_Invoked_By_Others")
                        {
                            $Change = "Added super-interface <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "If abstract methods from an added super-interface must be implemented by client then it may be interrupted by <b>AbstractMethodError</b> exception.<br/><br/>Abstract method ".black_Name_S(htmlSpecChars($Problems{"Invoked"}))." from the added super-interface is called by the method ".black_Name($Problems{"InvokedBy"}, 2)." in 2nd library version and may not be implemented by old clients.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method in <b>".htmlSpecChars($Target)."</b>.";
                            }
                        }
                        elsif($Kind eq "Abstract_Class_Added_Super_Interface_With_Implemented_Methods")
                        {
                            $Change = "Added super-interface <b>".htmlSpecChars($Target)."</b>.";
                            $Effect = "No effect.";
                        }
                        elsif($Kind eq "Interface_Added_Super_Interface")
                        {
                            $Change = "Added super-interface <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method in <b>".htmlSpecChars($Target)."</b>.";
                            }
                        }
                        elsif($Kind eq "Interface_Added_Super_Interface_Used_By_Others")
                        {
                            $Change = "Added super-interface <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary")
                            {
                                $Effect = "If abstract methods from an added super-interface must be implemented by client then it may be interrupted by <b>AbstractMethodError</b> exception.<br/><br/>Abstract method ".black_Name_S(htmlSpecChars($Problems{"Invoked"}))." from the added super-interface is called by the method ".black_Name($Problems{"InvokedBy"}, 2)." in 2nd library version and may not be implemented by old clients.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method in <b>".htmlSpecChars($Target)."</b>.";
                            }
                        }
                        elsif($Kind eq "Interface_Added_Super_Interface_With_Implemented_Methods")
                        {
                            $Change = "Added super-interface <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "No effect.";
                            }
                        }
                        elsif($Kind eq "Interface_Added_Super_Constant_Interface")
                        {
                            $Change = "Added super-interface <b>".htmlSpecChars($Target)."</b> containing constants only.";
                            if($Level eq "Binary") {
                                $Effect = "A static field from a super-interface of a client class may hide a field (with the same name) inherited from a super-class and cause <b>IncompatibleClassChangeError</b> exception.";
                            }
                            else {
                                $Effect = "A static field from a super-interface of a client class may hide a field (with the same name) inherited from a super-class. Recompilation of a client class may be terminated with the message: reference to variable is ambiguous.";
                            }
                        }
                        elsif($Kind eq "Interface_Removed_Super_Interface"
                        or $Kind eq "Class_Removed_Super_Interface")
                        {
                            $Change = "Removed super-interface <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>NoSuchMethodError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find method in $Type_Type <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Interface_Removed_Super_Constant_Interface")
                        {
                            $Change = "Removed super-interface <b>".htmlSpecChars($Target)."</b> containing constants only.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find variable in $Type_Type <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Added_Super_Class")
                        {
                            $Change = "Added super-class <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A static field from a super-interface of a client class may hide a field (with the same name) inherited from new super-class and cause <b>IncompatibleClassChangeError</b> exception.";
                            }
                            else {
                                $Effect = "A static field from a super-interface of a client class may hide a field (with the same name) inherited from new super-class. Recompilation of a client class may be terminated with the message: reference to variable is ambiguous.";
                            }
                        }
                        elsif($Kind eq "Abstract_Class_Added_Super_Abstract_Class")
                        {
                            $Change = "Added abstract super-class <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method in <b>".htmlSpecChars($Target)."</b>.";
                            }
                        }
                        elsif($Kind eq "Abstract_Class_Added_Super_Abstract_Class_Invoked_By_Others")
                        {
                            $Change = "Added abstract super-class <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "If abstract methods from an added super-class must be implemented by client then it may be interrupted by <b>AbstractMethodError</b> exception.<br/><br/>Abstract method ".black_Name_S(htmlSpecChars($Problems{"Invoked"}))." from the added abstract super-class is called by the method ".black_Name($Problems{"InvokedBy"}, 2)." in 2nd library version and may not be implemented by old clients.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: a client class C is not abstract and does not override abstract method in <b>".htmlSpecChars($Target)."</b>.";
                            }
                        }
                        elsif($Kind eq "Removed_Super_Class")
                        {
                            $Change = "Removed super-class <b>".htmlSpecChars($Target)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "Access of a client program to the fields or methods of the old super-class may be interrupted by <b>NoSuchFieldError</b> or <b>NoSuchMethodError</b> exceptions.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find variable (or method) in <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Changed_Super_Class")
                        {
                            $Change = "Superclass has been changed from <b>".htmlSpecChars($Old_Value)."</b> to <b>".htmlSpecChars($New_Value)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "1) Access of a client program to the fields or methods of the old super-class may be interrupted by <b>NoSuchFieldError</b> or <b>NoSuchMethodError</b> exceptions.<br/>2) A static field from a super-interface of a client class may hide a field (with the same name) inherited from new super-class and cause <b>IncompatibleClassChangeError</b> exception.";
                            }
                            else {
                                $Effect = "1) Recompilation of a client program may be terminated with the message: cannot find variable (or method) in <b>".htmlSpecChars($TypeName)."</b>.<br/>2) A static field from a super-interface of a client class may hide a field (with the same name) inherited from new super-class. Recompilation of a client class may be terminated with the message: reference to variable is ambiguous.";
                            }
                        }
                        elsif($Kind eq "Class_Added_Field")
                        {
                            $Change = "Field <b>$Target</b> has been added to this class.";
                            if($Level eq "Binary")
                            {
                                $Effect = "No effect.";
                                # $Effect .= "<br/><b>NOTE</b>: A static field from a super-interface of a client class may hide an added field (with the same name) inherited from the super-class of a client class and cause <b>IncompatibleClassChangeError</b> exception.";
                            }
                            else
                            {
                                $Effect = "No effect.";
                                # $Effect .= "<br/><b>NOTE</b>: A static field from a super-interface of a client class may hide an added field (with the same name) inherited from the super-class of a client class. Recompilation of a client class may be terminated with the message: reference to <b>$Target</b> is ambiguous.";
                            }
                        }
                        elsif($Kind eq "Interface_Added_Field")
                        {
                            $Change = "Field <b>$Target</b> has been added to this interface.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.<br/><b>NOTE</b>: An added static field from a super-interface of a client class may hide a field (with the same name) inherited from the super-class of a client class and cause <b>IncompatibleClassChangeError</b> exception.";
                            }
                            else {
                                $Effect = "No effect.<br/><b>NOTE</b>: An added static field from a super-interface of a client class may hide a field (with the same name) inherited from the super-class of a client class. Recompilation of a client class may be terminated with the message: reference to <b>$Target</b> is ambiguous.";
                            }
                        }
                        elsif($Kind eq "Renamed_Field")
                        {
                            $Change = "Field <b>$Target</b> has been renamed to <b>".htmlSpecChars($New_Value)."</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>NoSuchFieldError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find variable <b>$Target</b> in <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Renamed_Constant_Field")
                        {
                            if($Level eq "Binary") {
                                $Change = "Field <b>$Target</b> (".htmlSpecChars($Field_Type).") with the compile-time constant value <b>$Field_Value</b> has been renamed to <b>".htmlSpecChars($New_Value)."</b>.";
                                $Effect = "A client program may change behavior.";
                            }
                            else {
                                $Change = "Field <b>$Target</b> has been renamed to <b>".htmlSpecChars($New_Value)."</b>.";
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find variable <b>$Target</b> in <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Removed_NonConstant_Field")
                        {
                            $Change = "Field <b>$Target</b> of type ".htmlSpecChars($Field_Type)." has been removed from this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>NoSuchFieldError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find variable <b>$Target</b> in <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Removed_Constant_Field")
                        {
                            $Change = "Field <b>$Target</b> (".htmlSpecChars($Field_Type).") with the compile-time constant value <b>$Field_Value</b> has been removed from this $Type_Type.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may change behavior.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find variable <b>$Target</b> in <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Changed_Field_Type")
                        {
                            $Change = "Type of field <b>$Target</b> has been changed from <span class='nowrap'><b>".htmlSpecChars($Old_Value)."</b></span> to <span class='nowrap'><b>".htmlSpecChars($New_Value)."</b></span>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>NoSuchFieldError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: incompatible types, found: <b>".htmlSpecChars($Old_Value)."</b>, required: <b>".htmlSpecChars($New_Value)."</b>.";
                            }
                        }
                        elsif($Kind eq "Changed_Field_Access")
                        {
                            $Change = "Access level of field <b>$Target</b> has been changed from <span class='nowrap'><b>$Old_Value</b></span> to <span class='nowrap'><b>$New_Value</b></span>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>IllegalAccessError</b> exception.";
                            }
                            else
                            {
                                if($New_Value eq "package-private") {
                                    $Effect = "Recompilation of a client program may be terminated with the message: <b>$Target</b> is not public in <b>".htmlSpecChars($TypeName)."</b>; cannot be accessed from outside package.";
                                }
                                else {
                                    $Effect = "Recompilation of a client program may be terminated with the message: <b>$Target</b> has <b>$New_Value</b> access in <b>".htmlSpecChars($TypeName)."</b>.";
                                }
                            }
                        }
                        elsif($Kind eq "Changed_Final_Field_Value")
                        { # Binary Only
                            $Change = "Value of final field <b>$Target</b> (<b>$Field_Type</b>) has been changed from <span class='nowrap'><b>".htmlSpecChars($Old_Value)."</b></span> to <span class='nowrap'><b>".htmlSpecChars($New_Value)."</b></span>.";
                            $Effect = "Old value of the field will be inlined to the client code at compile-time and will be used instead of a new one.";
                        }
                        elsif($Kind eq "Changed_Final_Version_Field_Value")
                        { # Binary Only
                            $Change = "Value of final field <b>$Target</b> (<b>$Field_Type</b>) has been changed from <span class='nowrap'><b>".htmlSpecChars($Old_Value)."</b></span> to <span class='nowrap'><b>".htmlSpecChars($New_Value)."</b></span>.";
                            $Effect = "Old value of the field will be inlined to the client code at compile-time and will be used instead of a new one.";
                        }
                        elsif($Kind eq "Field_Became_Final")
                        {
                            $Change = "Field <b>$Target</b> became <b>final</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>IllegalAccessError</b> exception when attempt to assign new values to the field.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot assign a value to final variable $Target.";
                            }
                        }
                        elsif($Kind eq "Field_Became_NonFinal")
                        { # Binary Only
                            $Change = "Field <b>$Target</b> became <b>non-final</b>.";
                            $Effect = "Old value of the field will be inlined to the client code at compile-time and will be used instead of a new one.";
                        }
                        elsif($Kind eq "NonConstant_Field_Became_Static")
                        { # Binary Only
                            $Change = "Non-final field <b>$Target</b> became <b>static</b>.";
                            $Effect = "A client program may be interrupted by <b>IncompatibleClassChangeError</b> exception.";
                        }
                        elsif($Kind eq "NonConstant_Field_Became_NonStatic")
                        {
                            if($Level eq "Binary") {
                                $Change = "Non-constant field <b>$Target</b> became <b>non-static</b>.";
                                $Effect = "A client program may be interrupted by <b>IncompatibleClassChangeError</b> exception.";
                            }
                            else {
                                $Change = "Field <b>$Target</b> became <b>non-static</b>.";
                                $Effect = "Recompilation of a client program may be terminated with the message: non-static variable <b>$Target</b> cannot be referenced from a static context.";
                            }
                        }
                        elsif($Kind eq "Constant_Field_Became_NonStatic")
                        { # Source Only
                            $Change = "Field <b>$Target</b> became <b>non-static</b>.";
                            $Effect = "Recompilation of a client program may be terminated with the message: non-static variable <b>$Target</b> cannot be referenced from a static context.";
                        }
                        elsif($Kind eq "Class_Became_Interface")
                        {
                            $Change = "This <b>class</b> became <b>interface</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>IncompatibleClassChangeError</b> or <b>InstantiationError</b> exception dependent on the usage of this class.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: <b>".htmlSpecChars($TypeName)."</b> is abstract; cannot be instantiated.";
                            }
                        }
                        elsif($Kind eq "Interface_Became_Class")
                        {
                            $Change = "This <b>interface</b> became <b>class</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>IncompatibleClassChangeError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: interface expected.";
                            }
                        }
                        elsif($Kind eq "Class_Became_Final")
                        {
                            $Change = "This class became <b>final</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>VerifyError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot inherit from final <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Class_Became_Abstract")
                        {
                            $Change = "This class became <b>abstract</b>.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>InstantiationError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: <b>".htmlSpecChars($TypeName)."</b> is abstract; cannot be instantiated.";
                            }
                        }
                        elsif($Kind eq "Removed_Class")
                        {
                            $Change = "This class has been removed.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>NoClassDefFoundError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find class <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Removed_Interface")
                        {
                            $Change = "This interface has been removed.";
                            if($Level eq "Binary") {
                                $Effect = "A client program may be interrupted by <b>NoClassDefFoundError</b> exception.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the message: cannot find class <b>".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        elsif($Kind eq "Removed_Annotation")
                        {
                            $Change = "This annotation type has been removed.";
                            if($Level eq "Binary") {
                                $Effect = "No effect.";
                            }
                            else {
                                $Effect = "Recompilation of a client program may be terminated with the error message: cannot find symbol <b>\@".htmlSpecChars($TypeName)."</b>.";
                            }
                        }
                        if($Change)
                        {
                            $TYPE_REPORT .= "<tr><th>$ProblemNum</th><td>".$Change."</td><td>".$Effect."</td></tr>\n";
                            $ProblemNum += 1;
                            $ProblemsNum += 1;
                            $Kinds_Locations{$Kind}{$Location} = 1;
                        }
                    }
                }
                $ProblemNum -= 1;
                if($TYPE_REPORT)
                {
                    my $Affected = "";
                    if(not defined $TypeInfo{1}{$TypeId}{"Annotation"}) {
                        $Affected = getAffectedMethods($Level, $TypeName, \%Kinds_Locations);
                    }
                    
                    my $ShowType = $TypeName;
                    if($NameSpace)
                    {
                        $TYPE_REPORT = cut_Namespace($TYPE_REPORT, $NameSpace);
                        $ShowType = cut_Namespace($ShowType, $NameSpace);
                        $Affected = cut_Namespace($Affected, $NameSpace);
                    }
                    
                    $TYPE_PROBLEMS .= $ContentSpanStart."<span class='ext'>[+]</span> ".htmlSpecChars($ShowType);
                    if($OldStyle) {
                        $TYPE_PROBLEMS .= " ($ProblemNum)";
                    }
                    else {
                        $TYPE_PROBLEMS .= " <span".getStyle("T", $TargetSeverity, $ProblemNum).">&nbsp;$ProblemNum&nbsp;</span>";
                    }
                    $TYPE_PROBLEMS .= $ContentSpanEnd."<br/>\n";
                    $TYPE_PROBLEMS .= $ContentDivStart."<table class='ptable'><tr>";
                    $TYPE_PROBLEMS .= "<th width='2%'></th><th width='47%'>Change</th><th>Effect</th>";
                    $TYPE_PROBLEMS .= "</tr>$TYPE_REPORT</table>".$Affected."<br/><br/>$ContentDivEnd\n";
                }
            }
            
            $TYPE_PROBLEMS .= "<br/>";
        }
    }
    if($TYPE_PROBLEMS)
    {
        $TYPE_PROBLEMS = insertIDs($TYPE_PROBLEMS);
        
        my $Title = "Problems with Data Types, $TargetSeverity Severity";
        if($TargetSeverity eq "Safe")
        { # Safe Changes
            $Title = "Other Changes in Data Types";
        }
        if($OldStyle) {
            $TYPE_PROBLEMS = "<h2>$Title ($ProblemsNum)</h2><hr/>\n".$TYPE_PROBLEMS;
        }
        else {
            $TYPE_PROBLEMS = "<h2>$Title <span".getStyle("T", $TargetSeverity, $ProblemsNum).">&nbsp;$ProblemsNum&nbsp;</span></h2><hr/>\n".$TYPE_PROBLEMS;
        }
        $TYPE_PROBLEMS = "<a name='".get_Anchor("Type", $Level, $TargetSeverity)."'></a>\n".$TYPE_PROBLEMS;
        $TYPE_PROBLEMS .= $TOP_REF."<br/>\n";
    }
    return $TYPE_PROBLEMS;
}

sub cut_Namespace($$)
{
    my ($N, $Ns) = @_;
    $N=~s/(\W|\A)\Q$Ns\E\.(\w)/$1$2/g;
    return $N;
}

sub getAffectedMethods($$$)
{
    my ($Level, $Target_TypeName, $Kinds_Locations) = @_;
    
    my $LIMIT = 10;
    if(defined $AffectLimit) {
        $LIMIT = $AffectLimit;
    }
    
    my @Kinds = sort keys(%{$Kinds_Locations});
    my %KLocs = ();
    foreach my $Kind (@Kinds)
    {
        my @Locs = sort {$a=~/retval/ cmp $b=~/retval/} sort {length($a)<=>length($b)} sort keys(%{$Kinds_Locations->{$Kind}});
        $KLocs{$Kind} = \@Locs;
    }
    
    my %SymLocKind = ();
    foreach my $Method (sort keys(%{$TypeProblemsIndex{$Target_TypeName}}))
    {
        if($Method eq ".client_method") {
            next;
        }
        
        foreach my $Kind (@Kinds)
        {
            foreach my $Loc (@{$KLocs{$Kind}})
            {
                if(not defined $CompatProblems{$Method}{$Kind}{$Loc}) {
                    next;
                }
                
                my $Type_Name = $CompatProblems{$Method}{$Kind}{$Loc}{"Type_Name"};
                if($Type_Name ne $Target_TypeName) {
                    next;
                }
                
                $SymLocKind{$Method}{$Loc}{$Kind} = 1;
                last;
            }
        }
    }
    
    %KLocs = (); # clear
    
    if(not keys(%SymLocKind)) {
        return "";
    }
    
    my %SymSel = ();
    my $Num = 0;
    foreach my $Method (sort keys(%SymLocKind))
    {
        LOOP: foreach my $Loc (sort {$a=~/retval/ cmp $b=~/retval/} sort {length($a)<=>length($b)} sort keys(%{$SymLocKind{$Method}}))
        {
            foreach my $Kind (sort keys(%{$SymLocKind{$Method}{$Loc}}))
            {
                $SymSel{$Method}{"Loc"} = $Loc;
                $SymSel{$Method}{"Kind"} = $Kind;
                last LOOP;
            }
        }
        
        $Num += 1;
        
        if($Num>=$LIMIT) {
            last;
        }
    }
    
    my $Affected = "";
    
    foreach my $Method (sort {lc($a) cmp lc($b)} keys(%SymSel))
    {
        my $Kind = $SymSel{$Method}{"Kind"};
        my $Loc = $SymSel{$Method}{"Loc"};
        
        my $Desc = getAffectDesc($Method, $Kind, $Loc, $Level);
        my $PName = getParamName($Loc);
        my $Pos = getParamPos($PName, $Method, 1);
        
        $Affected .= "<span class='iname_a'>".get_Signature($Method, 1, "HTML|Italic|Param|Class|Target=".$Pos)."</span><br/>";
        $Affected .= "<div class='affect'>".$Desc."</div>\n";
    }
    
    if(keys(%SymLocKind)>$LIMIT) {
        $Affected .= " <b>...</b>\n<br/>\n"; # and others ...
    }
    
    $Affected = "<div class='affected'>".$Affected."</div>";
    if($Affected)
    {
        my $Num = keys(%SymLocKind);
        my $Per = show_number($Num*100/keys(%CheckedMethods));
        $Affected =  $ContentDivStart.$Affected.$ContentDivEnd;
        $Affected =  $ContentSpanStart_Affected."[+] affected methods: $Num ($Per\%)".$ContentSpanEnd.$Affected;
    }
    
    return ($Affected);
}

sub getAffectDesc($$$$)
{
    my ($Method, $Kind, $Location, $Level) = @_;
    my %Affect = %{$CompatProblems{$Method}{$Kind}{$Location}};
    my $New_Value = $Affect{"New_Value"};
    my $Type_Name = $Affect{"Type_Name"};
    my @Sentence_Parts = ();
    
    $Location=~s/\.[^.]+?\Z//;
    
    my %TypeAttr = get_Type($MethodInfo{1}{$Method}{"Class"}, 1);
    my $Type_Type = $TypeAttr{"Type"};
    
    my $ABSTRACT_M = $MethodInfo{1}{$Method}{"Abstract"}?" abstract":"";
    my $ABSTRACT_C = $TypeAttr{"Abstract"}?" abstract":"";
    my $METHOD_TYPE = $MethodInfo{1}{$Method}{"Constructor"}?"constructor":"method";
    
    if($Kind eq "Class_Overridden_Method" or $Kind eq "Class_Method_Moved_Up_Hierarchy") {
        return "Method '".highLight_Signature($New_Value, 2)."' will be called instead of this method in a client program.";
    }
    elsif($TypeProblems_Kind{$Level}{$Kind})
    {
        my %MInfo = %{$MethodInfo{1}{$Method}};
        
        if($Location eq "this") {
            return "This$ABSTRACT_M $METHOD_TYPE is from \'".htmlSpecChars($Type_Name)."\'$ABSTRACT_C $Type_Type.";
        }
        
        my $TypeID = undef;
        
        if($Location=~/retval/)
        { # return value
            if($Location=~/\./) {
                push(@Sentence_Parts, "Field \'".htmlSpecChars($Location)."\' in return value");
            }
            else {
                push(@Sentence_Parts, "Return value");
            }
            
            $TypeID = $MInfo{"Return"};
        }
        elsif($Location=~/this/)
        { # "this" reference
            push(@Sentence_Parts, "Field \'".htmlSpecChars($Location)."\' in the object");
            
            $TypeID = $MInfo{"Class"};
        }
        else
        { # parameters
            my $PName = getParamName($Location);
            my $PPos = getParamPos($PName, $Method, 1);
            
            if($Location=~/\./) {
                push(@Sentence_Parts, "Field \'".htmlSpecChars($Location)."\' in ".showPos($PPos)." parameter");
            }
            else {
                push(@Sentence_Parts, showPos($PPos)." parameter");
            }
            if($PName) {
                push(@Sentence_Parts, "\'$PName\'");
            }
            
            if(defined $MInfo{"Param"}) {
                $TypeID = $MInfo{"Param"}{$PPos}{"Type"};
            }
        }
        push(@Sentence_Parts, " of this$ABSTRACT_M method");
        
        my $Location_T = $Location;
        $Location_T=~s/\A\w+(\.|\Z)//; # location in type
        
        my $TypeID_Problem = $TypeID;
        if($Location_T) {
            $TypeID_Problem = getFieldType($Location_T, $TypeID, 1);
        }
        
        if($TypeInfo{1}{$TypeID_Problem}{"Name"} eq $Type_Name) {
            push(@Sentence_Parts, "has type \'".htmlSpecChars($Type_Name)."\'.");
        }
        else {
            push(@Sentence_Parts, "has base type \'".htmlSpecChars($Type_Name)."\'.");
        }
    }
    return join(" ", @Sentence_Parts);
}

sub getParamPos($$$)
{
    my ($Name, $Method, $LibVersion) = @_;
    
    if(defined $MethodInfo{$LibVersion}{$Method}
    and defined $MethodInfo{$LibVersion}{$Method}{"Param"})
    {
        my $Info = $MethodInfo{$LibVersion}{$Method};
        foreach (keys(%{$Info->{"Param"}}))
        {
            if($Info->{"Param"}{$_}{"Name"} eq $Name)
            {
                return $_;
            }
        }
    }
    
    return undef;
}

sub getParamName($)
{
    my $Loc = $_[0];
    $Loc=~s/\..*//g;
    return $Loc;
}

sub getFieldType($$$)
{
    my ($Location, $TypeId, $LibVersion) = @_;
    
    my @Fields = split(/\./, $Location);
    
    foreach my $Name (@Fields)
    {
        my %Info = get_BaseType($TypeId, $LibVersion);
        
        foreach my $N (keys(%{$Info{"Fields"}}))
        {
            if($N eq $Name)
            {
                $TypeId = $Info{"Fields"}{$N}{"Type"};
                last;
            }
        }
    }
    
    return $TypeId;
}

sub writeReport($$)
{
    my ($Level, $Report) = @_;
    my $RPath = getReportPath($Level);
    writeFile($RPath, $Report);
}

sub getRelPath($$)
{
    my ($A, $B) = @_;
    return abs2rel($A, get_dirname($B));
}

sub createReport()
{
    if($JoinReport)
    { # --stdout
        writeReport("Join", getReport("Join"));
    }
    elsif($DoubleReport)
    { # default
        writeReport("Binary", getReport("Binary"));
        writeReport("Source", getReport("Source"));
    }
    elsif($BinaryOnly)
    { # --binary
        writeReport("Binary", getReport("Binary"));
    }
    elsif($SourceOnly)
    { # --source
        writeReport("Source", getReport("Source"));
    }
}

sub getCssStyles()
{
    my $CssStyles = "
    body {
        font-family:Arial, sans-serif;
        background-color:White;
        color:Black;
    }
    hr {
        color:Black;
        background-color:Black;
        height:1px;
        border:0;
    }
    h1 {
        margin-bottom:0px;
        padding-bottom:0px;
        font-size:1.625em;
    }
    h2 {
        margin-bottom:0px;
        padding-bottom:0px;
        font-size:1.25em;
        white-space:nowrap;
    }
    div.symbols {
        color:#003E69;
    }
    div.symbols i {
        color:Brown;
    }
    span.section {
        font-weight:bold;
        cursor:pointer;
        color:#003E69;
        white-space:nowrap;
        margin-left:5px;
    }
    span:hover.section {
        color:#336699;
    }
    span.sect_aff {
        cursor:pointer;
        margin-left:7px;
        padding-left:15px;
        font-size:0.875em;
        color:#cc3300;
    }
    span.ext {
        font-weight:100;
    }
    span.jar {
        color:#cc3300;
        font-size:0.875em;
        font-weight:bold;
    }
    div.jar_list {
        padding-left:5px;
        font-size:0.94em;
    }
    span.pkg_t {
        color:#408080;
        font-size:0.875em;
    }
    span.pkg {
        color:#408080;
        font-size:0.875em;
        font-weight:bold;
    }
    span.cname {
        color:Green;
        font-size:0.875em;
        font-weight:bold;
    }
    span.iname_b {
        font-weight:bold;
        font-size:1.1em;
    }
    span.iname_a {
        color:#333333;
        font-weight:bold;
        font-size:0.94em;
    }
    span.sym_p {
        font-weight:normal;
        white-space:normal;
    }
    span.sym_p span {
        white-space:nowrap;
    }
    span.attr {
        color:Black;
        font-weight:100;
    }
    span.deprecated {
        color:Red;
        font-weight:bold;
        font-family:Monaco, monospace;
    }
    div.affect {
        padding-left:15px;
        padding-bottom:10px;
        font-size:0.87em;
        font-style:italic;
        line-height:0.75em;
    }
    div.affected {
        padding-left:30px;
        padding-top:10px;
    }
    table.ptable {
        border-collapse:collapse;
        border:1px outset black;
        line-height:1em;
        margin-left:15px;
        margin-top:3px;
        margin-bottom:3px;
        width:900px;
    }
    table.ptable td {
        border:1px solid Gray;
        padding: 3px;
        font-size:0.875em;
        text-align:left;
        vertical-align:top;
    }
    table.ptable th {
        background-color:#eeeeee;
        font-weight:bold;
        color:#333333;
        font-family:Verdana, Arial;
        font-size:0.875em;
        border:1px solid Gray;
        text-align:center;
        vertical-align:top;
        white-space:nowrap;
        padding: 3px;
    }
    table.summary {
        border-collapse:collapse;
        border:1px outset black;
    }
    table.summary th {
        background-color:#eeeeee;
        font-weight:100;
        text-align:left;
        font-size:0.94em;
        white-space:nowrap;
        border:1px inset Gray;
        padding: 3px;
    }
    table.summary td {
        text-align:right;
        white-space:nowrap;
        border:1px inset Gray;
        padding: 3px 5px 3px 10px;
    }
    span.mngl {
        padding-left:15px;
        font-size:0.875em;
        cursor:text;
        color:#444444;
    }
    span.color_p {
        font-style:italic;
        color:Brown;
    }
    span.param {
        font-style:italic;
    }
    span.focus_p {
        font-style:italic;
        background-color:#DCDCDC;
    }
    span.nowrap {
        white-space:nowrap;
    }
    .passed {
        background-color:#CCFFCC;
        font-weight:100;
    }
    .warning {
        background-color:#F4F4AF;
        font-weight:100;
    }
    .failed {
        background-color:#FFCCCC;
        font-weight:100;
    }
    .new {
        background-color:#C6DEFF;
        font-weight:100;
    }
    
    .compatible {
        background-color:#CCFFCC;
        font-weight:100;
    }
    .almost_compatible {
        background-color:#FFDAA3;
        font-weight:100;
    }
    .incompatible {
        background-color:#FFCCCC;
        font-weight:100;
    }
    .gray {
        background-color:#DCDCDC;
        font-weight:100;
    }
    
    .top_ref {
        font-size:0.69em;
    }
    .footer {
        font-size:0.75em;
    }";
    
    if($JoinReport or $ExternCss)
    {
        $CssStyles .= "
    .tabset {
        float:left;
    }
    a.tab {
        border:1px solid Black;
        float:left;
        margin:0px 5px -1px 0px;
        padding:3px 5px 3px 5px;
        position:relative;
        font-size:0.875em;
        background-color:#DDD;
        text-decoration:none;
        color:Black;
    }
    a.disabled:hover
    {
        color:Black;
        background:#EEE;
    }
    a.active:hover
    {
        color:Black;
        background:White;
    }
    a.active {
        border-bottom-color:White;
        background-color:White;
    }
    div.tab {
        border-top:1px solid Black;
        padding:0px;
        width:100%;
        clear:both;
    }";
    }
    
    return $CssStyles;
}

sub getJsScript()
{
    my $JScripts = "
    function sC(header, id)
    {
        e = document.getElementById(id);
        if(e.style.display == 'none')
        {
            e.style.display = 'block';
            e.style.visibility = 'visible';
            header.innerHTML = header.innerHTML.replace(/\\\[[^0-9 ]\\\]/gi,\"[&minus;]\");
        }
        else
        {
            e.style.display = 'none';
            e.style.visibility = 'hidden';
            header.innerHTML = header.innerHTML.replace(/\\\[[^0-9 ]\\\]/gi,\"[+]\");
        }
    }";
    
    if($JoinReport or $ExternJs)
    {
        $JScripts .= "
    function initTabs()
    {
        var url = window.location.href;
        if(url.indexOf('_Source_')!=-1 || url.indexOf('#Source')!=-1)
        {
            var tab1 = document.getElementById('BinaryID');
            var tab2 = document.getElementById('SourceID');
            tab1.className='tab disabled';
            tab2.className='tab active';
        }
        var sets = document.getElementsByTagName('div');
        for (var i = 0; i < sets.length; i++)
        {
            if (sets[i].className.indexOf('tabset') != -1)
            {
                var tabs = [];
                var links = sets[i].getElementsByTagName('a');
                for (var j = 0; j < links.length; j++)
                {
                    if (links[j].className.indexOf('tab') != -1)
                    {
                        tabs.push(links[j]);
                        links[j].tabs = tabs;
                        var tab = document.getElementById(links[j].href.substr(links[j].href.indexOf('#') + 1));
                        //reset all tabs on start
                        if (tab)
                        {
                            if (links[j].className.indexOf('active')!=-1) {
                                tab.style.display = 'block';
                            }
                            else {
                                tab.style.display = 'none';
                            }
                        }
                        links[j].onclick = function()
                        {
                            var tab = document.getElementById(this.href.substr(this.href.indexOf('#') + 1));
                            if (tab)
                            {
                                //reset all tabs before change
                                for (var k = 0; k < this.tabs.length; k++)
                                {
                                    document.getElementById(this.tabs[k].href.substr(this.tabs[k].href.indexOf('#') + 1)).style.display = 'none';
                                    this.tabs[k].className = this.tabs[k].className.replace('active', 'disabled');
                                }
                                this.className = 'tab active';
                                tab.style.display = 'block';
                                // window.location.hash = this.id.replace('ID', '');
                                return false;
                            }
                        }
                    }
                }
            }
        }
        if(url.indexOf('#')!=-1) {
            location.href=location.href;
        }
    }
    if (window.addEventListener) window.addEventListener('load', initTabs, false);
    else if (window.attachEvent) window.attachEvent('onload', initTabs);";
    }
    
    return $JScripts;
}

sub getReport($)
{
    my $Level = $_[0];
    
    my $CssStyles = getCssStyles();
    my $JScripts = getJsScript();
    
    if(defined $ExternCss)
    {
        $CssStyles=~s/\n    /\n/g;
        writeFile($ExternCss, $CssStyles);
    }
    
    if(defined $ExternJs)
    {
        $JScripts=~s/\n    /\n/g;
        writeFile($ExternJs, $JScripts);
    }
    
    if($Level eq "Join")
    {
        my $Title = "$TargetTitle: ".$Descriptor{1}{"Version"}." to ".$Descriptor{2}{"Version"}." compatibility report";
        my $Keywords = "$TargetTitle, compatibility";
        my $Description = "Compatibility report for the $TargetTitle library between ".$Descriptor{1}{"Version"}." and ".$Descriptor{2}{"Version"}." versions";
        my ($BSummary, $BMetaData) = get_Summary("Binary");
        my ($SSummary, $SMetaData) = get_Summary("Source");
        my $Report = "<!-\- $BMetaData -\->\n<!-\- $SMetaData -\->\n".composeHTML_Head($Level, $Title, $Keywords, $Description, $CssStyles, $JScripts)."<body><a name='Source'></a><a name='Binary'></a><a name='Top'></a>";
        $Report .= get_Report_Header("Join")."
        <br/><div class='tabset'>
        <a id='BinaryID' href='#BinaryTab' class='tab active'>Binary<br/>Compatibility</a>
        <a id='SourceID' href='#SourceTab' style='margin-left:3px' class='tab disabled'>Source<br/>Compatibility</a>
        </div>";
        $Report .= "<div id='BinaryTab' class='tab'>\n$BSummary\n".get_Report_Added("Binary").get_Report_Removed("Binary").get_Report_Problems("High", "Binary").get_Report_Problems("Medium", "Binary").get_Report_Problems("Low", "Binary").get_Report_Problems("Safe", "Binary").get_SourceInfo()."<br/><br/><br/></div>";
        $Report .= "<div id='SourceTab' class='tab'>\n$SSummary\n".get_Report_Added("Source").get_Report_Removed("Source").get_Report_Problems("High", "Source").get_Report_Problems("Medium", "Source").get_Report_Problems("Low", "Source").get_Report_Problems("Safe", "Source").get_SourceInfo()."<br/><br/><br/></div>";
        $Report .= getReportFooter();
        $Report .= "\n</body></html>";
        return $Report;
    }
    else
    {
        my ($Summary, $MetaData) = get_Summary($Level);
        my $Title = "$TargetTitle: ".$Descriptor{1}{"Version"}." to ".$Descriptor{2}{"Version"}." ".lc($Level)." compatibility report";
        my $Keywords = "$TargetTitle, ".lc($Level).", compatibility";
        my $Description = "$Level compatibility report for the $TargetTitle library between ".$Descriptor{1}{"Version"}." and ".$Descriptor{2}{"Version"}." versions";
        
        my $Report = "<!-\- $MetaData -\->\n".composeHTML_Head($Level, $Title, $Keywords, $Description, $CssStyles, $JScripts)."<body><a name='Top'></a>";
        $Report .= get_Report_Header($Level)."\n".$Summary."\n";
        $Report .= get_Report_Added($Level).get_Report_Removed($Level);
        $Report .= get_Report_Problems("High", $Level).get_Report_Problems("Medium", $Level).get_Report_Problems("Low", $Level).get_Report_Problems("Safe", $Level);
        $Report .= get_SourceInfo()."<br/><br/><br/>\n";
        $Report .= getReportFooter();
        $Report .= "\n</body></html>";
        return $Report;
    }
}

sub getReportFooter()
{
    my $Footer = "";
    $Footer .= "<hr/>";
    $Footer .= "<div class='footer' align='right'><i>Generated by ";
    $Footer .= "<a href='".$HomePage{"Dev"}."'>Java API Compliance Checker</a> $TOOL_VERSION &#160;";
    $Footer .= "</i></div>";
    $Footer .= "<br/>";
    return $Footer;
}

sub get_Report_Problems($$)
{
    my ($Priority, $Level) = @_;
    my $Report = get_Report_TypeProblems($Priority, $Level);
    if(my $MProblems = get_Report_MethodProblems($Priority, $Level)) {
        $Report .= $MProblems;
    }
    if($Report)
    {
        if($JoinReport)
        {
            if($Priority eq "Safe") {
                $Report = "<a name=\'Other_".$Level."_Changes\'></a>".$Report;
            }
            else {
                $Report = "<a name=\'".$Priority."_Risk_".$Level."_Problems\'></a>".$Report;
            }
        }
        else
        {
            if($Priority eq "Safe") {
                $Report = "<a name=\'Other_Changes\'></a>".$Report;
            }
            else {
                $Report = "<a name=\'".$Priority."_Risk_Problems\'></a>".$Report;
            }
        }
    }
    return $Report;
}

sub composeHTML_Head($$$$$$)
{
    my ($Level, $Title, $Keywords, $Description, $Styles, $Scripts) = @_;
    
    my $Head = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
    $Head .= "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n";
    $Head .= "<head>\n";
    $Head .= "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n";
    $Head .= "<meta name=\"keywords\" content=\"$Keywords\" />\n";
    $Head .= "<meta name=\"description\" content=\"$Description\" />\n";
    
    my $RPath = getReportPath($Level);
    
    if(defined $ExternCss) {
        $Head .= "<link rel=\"stylesheet\" type=\"text/css\" href=\"".getRelPath($ExternCss, $RPath)."\" />\n";
    }
    
    if(defined $ExternJs) {
        $Head .= "<script type=\"text/javascript\" src=\"".getRelPath($ExternJs, $RPath)."\"></script>\n";
    }
    
    $Head .= "<title>$Title</title>\n";
    
    if(not defined $ExternCss) {
        $Head .= "<style type=\"text/css\">\n$Styles\n</style>\n";
    }
    
    if(not defined $ExternJs) {
        $Head .= "<script type=\"text/javascript\" language=\"JavaScript\">\n<!--\n$Scripts\n-->\n</script>\n";
    }
    
    $Head .= "</head>\n";
    
    $Head=~s/\n    /\n/g;
    
    return $Head;
}

sub insertIDs($)
{
    my $Text = $_[0];
    while($Text=~/CONTENT_ID/)
    {
        if(int($Content_Counter)%2)
        {
            $ContentID -= 1;
        }
        $Text=~s/CONTENT_ID/c_$ContentID/;
        $ContentID += 1;
        $Content_Counter += 1;
    }
    return $Text;
}

sub readArchives($)
{
    my $LibVersion = $_[0];
    my @ArchivePaths = getArchives($LibVersion);
    if($#ArchivePaths==-1) {
        exitStatus("Error", "Java ARchives are not found in ".$Descriptor{$LibVersion}{"Version"});
    }
    printMsg("INFO", "reading classes ".$Descriptor{$LibVersion}{"Version"}." ...");
    $TYPE_ID = 0;
    
    foreach my $ArchivePath (sort {length($a)<=>length($b)} @ArchivePaths) {
        readArchive($LibVersion, $ArchivePath);
    }
    foreach my $TName (keys(%{$TName_Tid{$LibVersion}}))
    {
        my $Tid = $TName_Tid{$LibVersion}{$TName};
        if(not $TypeInfo{$LibVersion}{$Tid}{"Type"})
        {
            if($TName=~/\A(void|boolean|char|byte|short|int|float|long|double)\Z/) {
                $TypeInfo{$LibVersion}{$Tid}{"Type"} = "primitive";
            }
            else {
                $TypeInfo{$LibVersion}{$Tid}{"Type"} = "class";
            }
        }
    }
    
    foreach my $Method (keys(%{$MethodInfo{$LibVersion}}))
    {
        $MethodInfo{$LibVersion}{$Method}{"Signature"} = get_Signature($Method, $LibVersion, "Full");
        $tr_name{$Method} = get_TypeName($MethodInfo{$LibVersion}{$Method}{"Class"}, $LibVersion).".".get_Signature($Method, $LibVersion, "Short");
    }
}

sub testSystem()
{
    printMsg("INFO", "\nverifying detectable Java library changes");
    
    my $LibName = "libsample_java";
    if(-d $LibName) {
        rmtree($LibName);
    }
    
    my $PackageName = "TestPackage";
    my $Path_v1 = "$LibName/$PackageName.v1/$PackageName";
    mkpath($Path_v1);
    
    my $Path_v2 = "$LibName/$PackageName.v2/$PackageName";
    mkpath($Path_v2);
    
    my $TestsPath = "$LibName/Tests";
    mkpath($TestsPath);
    
    # FirstCheckedException
    my $FirstCheckedException = "package $PackageName;
    public class FirstCheckedException extends Exception {
    }";
    writeFile($Path_v1."/FirstCheckedException.java", $FirstCheckedException);
    writeFile($Path_v2."/FirstCheckedException.java", $FirstCheckedException);
    
    # SecondCheckedException
    my $SecondCheckedException = "package $PackageName;
    public class SecondCheckedException extends Exception {
    }";
    writeFile($Path_v1."/SecondCheckedException.java", $SecondCheckedException);
    writeFile($Path_v2."/SecondCheckedException.java", $SecondCheckedException);
    
    # FirstUncheckedException
    my $FirstUncheckedException = "package $PackageName;
    public class FirstUncheckedException extends RuntimeException {
    }";
    writeFile($Path_v1."/FirstUncheckedException.java", $FirstUncheckedException);
    writeFile($Path_v2."/FirstUncheckedException.java", $FirstUncheckedException);
    
    # SecondUncheckedException
    my $SecondUncheckedException = "package $PackageName;
    public class SecondUncheckedException extends RuntimeException {
    }";
    writeFile($Path_v1."/SecondUncheckedException.java", $SecondUncheckedException);
    writeFile($Path_v2."/SecondUncheckedException.java", $SecondUncheckedException);
    
    # BaseAbstractClass
    my $BaseAbstractClass = "package $PackageName;
    public abstract class BaseAbstractClass {
        public Integer field;
        public Integer someMethod(Integer param) { return param; }
        public abstract Integer abstractMethod(Integer param);
    }";
    writeFile($Path_v1."/BaseAbstractClass.java", $BaseAbstractClass);
    writeFile($Path_v2."/BaseAbstractClass.java", $BaseAbstractClass);
    
    # Removed_Annotation
    writeFile($Path_v1."/RemovedAnnotation.java",
    "package $PackageName;
    public \@interface RemovedAnnotation {
    }");
    
    # Beta Annotation
    writeFile($Path_v1."/Beta.java",
    "package $PackageName;
    public \@interface Beta {
    }");
    
    writeFile($Path_v2."/Beta.java",
    "package $PackageName;
    public \@interface Beta {
    }");
    
    # BaseClass
    my $BaseClass = "package $PackageName;
    public class BaseClass {
        public Integer field;
        public Integer method(Integer param) { return param; }
    }";
    writeFile($Path_v1."/BaseClass.java", $BaseClass);
    writeFile($Path_v2."/BaseClass.java", $BaseClass);
    
    # BaseClass2
    my $BaseClass2 = "package $PackageName;
    public class BaseClass2 {
        public Integer field2;
        public Integer method2(Integer param) { return param; }
    }";
    writeFile($Path_v1."/BaseClass2.java", $BaseClass2);
    writeFile($Path_v2."/BaseClass2.java", $BaseClass2);
    
    # BaseInterface
    my $BaseInterface = "package $PackageName;
    public interface BaseInterface {
        public Integer field = 100;
        public Integer method(Integer param);
    }";
    writeFile($Path_v1."/BaseInterface.java", $BaseInterface);
    writeFile($Path_v2."/BaseInterface.java", $BaseInterface);
    
    # BaseInterface2
    my $BaseInterface2 = "package $PackageName;
    public interface BaseInterface2 {
        public Integer field2 = 100;
        public Integer method2(Integer param);
    }";
    writeFile($Path_v1."/BaseInterface2.java", $BaseInterface2);
    writeFile($Path_v2."/BaseInterface2.java", $BaseInterface2);
    
    # BaseConstantInterface
    my $BaseConstantInterface = "package $PackageName;
    public interface BaseConstantInterface {
        public Integer CONSTANT = 10;
        public Integer CONSTANT2 = 100;
    }";
    writeFile($Path_v1."/BaseConstantInterface.java", $BaseConstantInterface);
    writeFile($Path_v2."/BaseConstantInterface.java", $BaseConstantInterface);
    
    # Removed_Method (Beta method)
    writeFile($Path_v1."/RemovedBetaMethod.java",
    "package $PackageName;
    public class RemovedBetaMethod
    {
        \@Beta
        public Integer someMethod() {
            return 0;
        }
    }");
    writeFile($Path_v2."/RemovedBetaMethod.java",
    "package $PackageName;
    public class RemovedBetaMethod {
    }");
    
    # Removed_Method (from Beta class)
    writeFile($Path_v1."/RemovedMethodFromBetaClass.java",
    "package $PackageName;
    \@Beta
    public class RemovedMethodFromBetaClass
    {
        public Integer someMethod() {
            return 0;
        }
    }");
    writeFile($Path_v2."/RemovedMethodFromBetaClass.java",
    "package $PackageName;
    \@Beta
    public class RemovedMethodFromBetaClass {
    }");
    
    # Removed_Class (Beta)
    writeFile($Path_v1."/RemovedBetaClass.java",
    "package $PackageName;
    \@Beta
    public class RemovedBetaClass
    {
        public Integer someMethod() {
            return 0;
        }
    }");
    
    # Abstract_Method_Added_Checked_Exception
    writeFile($Path_v1."/AbstractMethodAddedCheckedException.java",
    "package $PackageName;
    public abstract class AbstractMethodAddedCheckedException {
        public abstract Integer someMethod() throws FirstCheckedException;
    }");
    writeFile($Path_v2."/AbstractMethodAddedCheckedException.java",
    "package $PackageName;
    public abstract class AbstractMethodAddedCheckedException {
        public abstract Integer someMethod() throws FirstCheckedException, SecondCheckedException;
    }");
    
    # Abstract_Method_Removed_Checked_Exception
    writeFile($Path_v1."/AbstractMethodRemovedCheckedException.java",
    "package $PackageName;
    public abstract class AbstractMethodRemovedCheckedException {
        public abstract Integer someMethod() throws FirstCheckedException, SecondCheckedException;
    }");
    writeFile($Path_v2."/AbstractMethodRemovedCheckedException.java",
    "package $PackageName;
    public abstract class AbstractMethodRemovedCheckedException {
        public abstract Integer someMethod() throws FirstCheckedException;
    }");
    
    # NonAbstract_Method_Added_Checked_Exception
    writeFile($Path_v1."/NonAbstractMethodAddedCheckedException.java",
    "package $PackageName;
    public class NonAbstractMethodAddedCheckedException {
        public Integer someMethod() throws FirstCheckedException {
            return 10;
        }
    }");
    writeFile($Path_v2."/NonAbstractMethodAddedCheckedException.java",
    "package $PackageName;
    public class NonAbstractMethodAddedCheckedException {
        public Integer someMethod() throws FirstCheckedException, SecondCheckedException {
            return 10;
        }
    }");
    
    # NonAbstract_Method_Removed_Checked_Exception
    writeFile($Path_v1."/NonAbstractMethodRemovedCheckedException.java",
    "package $PackageName;
    public class NonAbstractMethodRemovedCheckedException {
        public Integer someMethod() throws FirstCheckedException, SecondCheckedException {
            return 10;
        }
    }");
    writeFile($Path_v2."/NonAbstractMethodRemovedCheckedException.java",
    "package $PackageName;
    public class NonAbstractMethodRemovedCheckedException {
        public Integer someMethod() throws FirstCheckedException {
            return 10;
        }
    }");
    
    # Added_Unchecked_Exception
    writeFile($Path_v1."/AddedUncheckedException.java",
    "package $PackageName;
    public class AddedUncheckedException {
        public Integer someMethod() throws FirstUncheckedException {
            return 10;
        }
    }");
    writeFile($Path_v2."/AddedUncheckedException.java",
    "package $PackageName;
    public class AddedUncheckedException {
        public Integer someMethod() throws FirstUncheckedException, SecondUncheckedException, NullPointerException {
            return 10;
        }
    }");
    
    # Removed_Unchecked_Exception
    writeFile($Path_v1."/RemovedUncheckedException.java",
    "package $PackageName;
    public class RemovedUncheckedException {
        public Integer someMethod() throws FirstUncheckedException, SecondUncheckedException, NullPointerException {
            return 10;
        }
    }");
    writeFile($Path_v2."/RemovedUncheckedException.java",
    "package $PackageName;
    public class RemovedUncheckedException {
        public Integer someMethod() throws FirstUncheckedException {
            return 10;
        }
    }");
    
    # Changed_Method_Return_From_Void
    writeFile($Path_v1."/ChangedMethodReturnFromVoid.java",
    "package $PackageName;
    public class ChangedMethodReturnFromVoid {
        public void changedMethod(Integer param1, String[] param2) { }
    }");
    writeFile($Path_v2."/ChangedMethodReturnFromVoid.java",
    "package $PackageName;
    public class ChangedMethodReturnFromVoid {
        public Integer changedMethod(Integer param1, String[] param2){
            return param1;
        }
    }");
    
    # Added_Method
    writeFile($Path_v1."/AddedMethod.java",
    "package $PackageName;
    public class AddedMethod {
        public Integer field = 100;
    }");
    writeFile($Path_v2."/AddedMethod.java",
    "package $PackageName;
    public class AddedMethod {
        public Integer field = 100;
        public Integer addedMethod(Integer param1, String[] param2) { return param1; }
        public static String[] addedStaticMethod(String[] param) { return param; }
    }");
    
    # Added_Method (Constructor)
    writeFile($Path_v1."/AddedConstructor.java",
    "package $PackageName;
    public class AddedConstructor {
        public Integer field = 100;
    }");
    writeFile($Path_v2."/AddedConstructor.java",
    "package $PackageName;
    public class AddedConstructor {
        public Integer field = 100;
        public AddedConstructor() { }
        public AddedConstructor(Integer x, String y) { }
    }");
    
    # Class_Added_Field
    writeFile($Path_v1."/ClassAddedField.java",
    "package $PackageName;
    public class ClassAddedField {
        public Integer otherField;
    }");
    writeFile($Path_v2."/ClassAddedField.java",
    "package $PackageName;
    public class ClassAddedField {
        public Integer addedField;
        public Integer otherField;
    }");
    
    # Interface_Added_Field
    writeFile($Path_v1."/InterfaceAddedField.java",
    "package $PackageName;
    public interface InterfaceAddedField {
        public Integer method();
    }");
    writeFile($Path_v2."/InterfaceAddedField.java",
    "package $PackageName;
    public interface InterfaceAddedField {
        public Integer addedField = 100;
        public Integer method();
    }");
    
    # Removed_NonConstant_Field (Class)
    writeFile($Path_v1."/ClassRemovedField.java",
    "package $PackageName;
    public class ClassRemovedField {
        public Integer removedField;
        public Integer otherField;
    }");
    writeFile($Path_v2."/ClassRemovedField.java",
    "package $PackageName;
    public class ClassRemovedField {
        public Integer otherField;
    }");
    
    writeFile($TestsPath."/Test_ClassRemovedField.java",
    "import $PackageName.*;
    public class Test_ClassRemovedField {
        public static void main(String[] args) {
            ClassRemovedField X = new ClassRemovedField();
            Integer Copy = X.removedField;
        }
    }");
    
    writeFile($TestsPath."/Test_RemovedAnnotation.java",
    "import $PackageName.*;
    public class Test_RemovedAnnotation {
        public static void main(String[] args) {
            testMethod();
        }
        
        \@RemovedAnnotation
        static void testMethod() {
        }
    }");
    
    # Removed_Constant_Field (Interface)
    writeFile($Path_v1."/InterfaceRemovedConstantField.java",
    "package $PackageName;
    public interface InterfaceRemovedConstantField {
        public String someMethod();
        public int removedField_Int = 1000;
        public String removedField_Str = \"Value\";
    }");
    writeFile($Path_v2."/InterfaceRemovedConstantField.java",
    "package $PackageName;
    public interface InterfaceRemovedConstantField {
        public String someMethod();
    }");
    
    # Removed_NonConstant_Field (Interface)
    writeFile($Path_v1."/InterfaceRemovedField.java",
    "package $PackageName;
    public interface InterfaceRemovedField {
        public String someMethod();
        public BaseClass removedField = new BaseClass();
    }");
    writeFile($Path_v2."/InterfaceRemovedField.java",
    "package $PackageName;
    public interface InterfaceRemovedField {
        public String someMethod();
    }");
    
    # Renamed_Field
    writeFile($Path_v1."/RenamedField.java",
    "package $PackageName;
    public class RenamedField {
        public String oldName;
    }");
    writeFile($Path_v2."/RenamedField.java",
    "package $PackageName;
    public class RenamedField {
        public String newName;
    }");
    
    # Renamed_Constant_Field
    writeFile($Path_v1."/RenamedConstantField.java",
    "package $PackageName;
    public class RenamedConstantField {
        public final String oldName = \"Value\";
    }");
    writeFile($Path_v2."/RenamedConstantField.java",
    "package $PackageName;
    public class RenamedConstantField {
        public final String newName = \"Value\";
    }");
    
    # Changed_Field_Type
    writeFile($Path_v1."/ChangedFieldType.java",
    "package $PackageName;
    public class ChangedFieldType {
        public String fieldName;
    }");
    writeFile($Path_v2."/ChangedFieldType.java",
    "package $PackageName;
    public class ChangedFieldType {
        public Integer fieldName;
    }");
    
    # Changed_Field_Access
    writeFile($Path_v1."/ChangedFieldAccess.java",
    "package $PackageName;
    public class ChangedFieldAccess {
        public String fieldName;
    }");
    writeFile($Path_v2."/ChangedFieldAccess.java",
    "package $PackageName;
    public class ChangedFieldAccess {
        private String fieldName;
    }");
    
    # Changed_Final_Field_Value
    writeFile($Path_v1."/ChangedFinalFieldValue.java",
    "package $PackageName;
    public class ChangedFinalFieldValue {
        public final int field = 1;
        public final String field2 = \" \";
    }");
    writeFile($Path_v2."/ChangedFinalFieldValue.java",
    "package $PackageName;
    public class ChangedFinalFieldValue {
        public final int field = 2;
        public final String field2 = \"newValue\";
    }");
    
    # NonConstant_Field_Became_Static
    writeFile($Path_v1."/NonConstantFieldBecameStatic.java",
    "package $PackageName;
    public class NonConstantFieldBecameStatic {
        public String fieldName;
    }");
    writeFile($Path_v2."/NonConstantFieldBecameStatic.java",
    "package $PackageName;
    public class NonConstantFieldBecameStatic {
        public static String fieldName;
    }");
    
    # NonConstant_Field_Became_NonStatic
    writeFile($Path_v1."/NonConstantFieldBecameNonStatic.java",
    "package $PackageName;
    public class NonConstantFieldBecameNonStatic {
        public static String fieldName;
    }");
    writeFile($Path_v2."/NonConstantFieldBecameNonStatic.java",
    "package $PackageName;
    public class NonConstantFieldBecameNonStatic {
        public String fieldName;
    }");
    
    # Constant_Field_Became_NonStatic
    writeFile($Path_v1."/ConstantFieldBecameNonStatic.java",
    "package $PackageName;
    public class ConstantFieldBecameNonStatic {
        public final static String fieldName = \"Value\";
    }");
    writeFile($Path_v2."/ConstantFieldBecameNonStatic.java",
    "package $PackageName;
    public class ConstantFieldBecameNonStatic {
        public final String fieldName = \"Value\";
    }");
    
    # Field_Became_Final
    writeFile($Path_v1."/FieldBecameFinal.java",
    "package $PackageName;
    public class FieldBecameFinal {
        public String fieldName;
    }");
    writeFile($Path_v2."/FieldBecameFinal.java",
    "package $PackageName;
    public class FieldBecameFinal {
        public final String fieldName = \"Value\";
    }");
    
    # Field_Became_NonFinal
    writeFile($Path_v1."/FieldBecameNonFinal.java",
    "package $PackageName;
    public class FieldBecameNonFinal {
        public final String fieldName = \"Value\";
    }");
    writeFile($Path_v2."/FieldBecameNonFinal.java",
    "package $PackageName;
    public class FieldBecameNonFinal {
        public String fieldName;
    }");
    
    # Removed_Method
    writeFile($Path_v1."/RemovedMethod.java",
    "package $PackageName;
    public class RemovedMethod {
        public Integer field = 100;
        public Integer removedMethod(Integer param1, String param2) { return param1; }
        public static Integer removedStaticMethod(Integer param) { return param; }
    }");
    writeFile($Path_v2."/RemovedMethod.java",
    "package $PackageName;
    public class RemovedMethod {
        public Integer field = 100;
    }");
    
    # Removed_Method (move up to java.lang.Object)
    writeFile($Path_v1."/MoveUpToJavaLangObject.java",
    "package $PackageName;
    public class MoveUpToJavaLangObject extends java.lang.Object {
        public int hashCode() { return 0; }
    }");
    writeFile($Path_v2."/MoveUpToJavaLangObject.java",
    "package $PackageName;
    public class MoveUpToJavaLangObject extends java.lang.Object {
    }");
    
    writeFile($TestsPath."/Test_MoveUpToJavaLangObject.java",
    "import $PackageName.*;
    public class Test_MoveUpToJavaLangObject {
        public static void main(String[] args) {
            MoveUpToJavaLangObject X = new MoveUpToJavaLangObject();
            int R = X.hashCode();
        }
    }");
    
    # Removed_Method (Deprecated)
    writeFile($Path_v1."/RemovedDeprecatedMethod.java",
    "package $PackageName;
    public class RemovedDeprecatedMethod {
        public Integer field = 100;
        public Integer otherMethod(Integer param) { return param; }
        \@Deprecated
        public Integer removedMethod(Integer param1, String param2) { return param1; }
    }");
    writeFile($Path_v2."/RemovedDeprecatedMethod.java",
    "package $PackageName;
    public class RemovedDeprecatedMethod {
        public Integer field = 100;
        public Integer otherMethod(Integer param) { return param; }
    }");
    
    # Interface_Removed_Abstract_Method
    writeFile($Path_v1."/InterfaceRemovedAbstractMethod.java",
    "package $PackageName;
    public interface InterfaceRemovedAbstractMethod extends BaseInterface, BaseInterface2 {
        public void removedMethod(Integer param1, java.io.ObjectOutput param2);
        public void someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceRemovedAbstractMethod.java",
    "package $PackageName;
    public interface InterfaceRemovedAbstractMethod extends BaseInterface, BaseInterface2 {
        public void someMethod(Integer param);
    }");
    
    # Interface_Added_Abstract_Method
    writeFile($Path_v1."/InterfaceAddedAbstractMethod.java",
    "package $PackageName;
    public interface InterfaceAddedAbstractMethod extends BaseInterface, BaseInterface2 {
        public void someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceAddedAbstractMethod.java",
    "package $PackageName;
    public interface InterfaceAddedAbstractMethod extends BaseInterface, BaseInterface2 {
        public void someMethod(Integer param);
        public Integer addedMethod(Integer param);
    }");
    
    # Interface_Added_Default_Method
    writeFile($Path_v1."/InterfaceAddedDefaultMethod.java",
    "package $PackageName;
    public interface InterfaceAddedDefaultMethod {
        public void someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceAddedDefaultMethod.java",
    "package $PackageName;
    public interface InterfaceAddedDefaultMethod {
        public void someMethod(Integer param);
        default Integer addedMethod(Integer param) { return 0; }
    }");
    
    # Method_Became_NonDefault
    writeFile($Path_v1."/MethodBecameNonDefault.java",
    "package $PackageName;
    public interface MethodBecameNonDefault {
        default Integer someMethod(Integer param) { return 0; }
    }");
    writeFile($Path_v2."/MethodBecameNonDefault.java",
    "package $PackageName;
    public interface MethodBecameNonDefault {
        public Integer someMethod(Integer param);
    }");
    
    writeFile($TestsPath."/Test_MethodBecameNonDefault.java",
    "import $PackageName.*;
    class Class_MethodBecameNonDefault implements MethodBecameNonDefault {
    };
    
    public class Test_MethodBecameNonDefault
    {
        public static void main(String[] args)
        {
            Class_MethodBecameNonDefault Obj = new Class_MethodBecameNonDefault();
            Integer Res = Obj.someMethod(0);
        }
    }");
    
    # Variable_Arity_To_Array
    writeFile($Path_v1."/VariableArityToArray.java",
    "package $PackageName;
    public class VariableArityToArray {
        public void someMethod(Integer x, String... y) { };
    }");
    writeFile($Path_v2."/VariableArityToArray.java",
    "package $PackageName;
    public class VariableArityToArray {
        public void someMethod(Integer x, String[] y) { };
    }");
    
    # Class_Became_Interface
    writeFile($Path_v1."/ClassBecameInterface.java",
    "package $PackageName;
    public class ClassBecameInterface extends BaseClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/ClassBecameInterface.java",
    "package $PackageName;
    public interface ClassBecameInterface extends BaseInterface, BaseInterface2 {
        public Integer someMethod(Integer param);
    }");
    
    # Added_Super_Class
    writeFile($Path_v1."/AddedSuperClass.java",
    "package $PackageName;
    public class AddedSuperClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/AddedSuperClass.java",
    "package $PackageName;
    public class AddedSuperClass extends BaseClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    
    # Abstract_Class_Added_Super_Abstract_Class
    writeFile($Path_v1."/AbstractClassAddedSuperAbstractClass.java",
    "package $PackageName;
    public abstract class AbstractClassAddedSuperAbstractClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/AbstractClassAddedSuperAbstractClass.java",
    "package $PackageName;
    public abstract class AbstractClassAddedSuperAbstractClass extends BaseAbstractClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    
    # Removed_Super_Class
    writeFile($Path_v1."/RemovedSuperClass.java",
    "package $PackageName;
    public class RemovedSuperClass extends BaseClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/RemovedSuperClass.java",
    "package $PackageName;
    public class RemovedSuperClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    
    # Changed_Super_Class
    writeFile($Path_v1."/ChangedSuperClass.java",
    "package $PackageName;
    public class ChangedSuperClass extends BaseClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/ChangedSuperClass.java",
    "package $PackageName;
    public class ChangedSuperClass extends BaseClass2 {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    
    # Abstract_Class_Added_Super_Interface
    writeFile($Path_v1."/AbstractClassAddedSuperInterface.java",
    "package $PackageName;
    public abstract class AbstractClassAddedSuperInterface implements BaseInterface {
        public Integer method(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/AbstractClassAddedSuperInterface.java",
    "package $PackageName;
    public abstract class AbstractClassAddedSuperInterface implements BaseInterface, BaseInterface2 {
        public Integer method(Integer param) {
            return param;
        }
    }");
    
    # Abstract_Class_Added_Super_Interface_With_Implemented_Methods
    writeFile($Path_v1."/AbstractClassAddedSuperInterfaceWithImplementedMethods.java",
    "package $PackageName;
    public abstract class AbstractClassAddedSuperInterfaceWithImplementedMethods implements BaseInterface {
        public Integer method(Integer param) {
            return param;
        }
        public Integer method2(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/AbstractClassAddedSuperInterfaceWithImplementedMethods.java",
    "package $PackageName;
    public abstract class AbstractClassAddedSuperInterfaceWithImplementedMethods implements BaseInterface, BaseInterface2 {
        public Integer method(Integer param) {
            return param;
        }
        public Integer method2(Integer param) {
            return param;
        }
    }");
    
    # Class_Removed_Super_Interface
    writeFile($Path_v1."/ClassRemovedSuperInterface.java",
    "package $PackageName;
    public class ClassRemovedSuperInterface implements BaseInterface, BaseInterface2 {
        public Integer method(Integer param) {
            return param;
        }
        public Integer method2(Integer param) {
            return param;
        }
    }");
    writeFile($Path_v2."/ClassRemovedSuperInterface.java",
    "package $PackageName;
    public class ClassRemovedSuperInterface implements BaseInterface {
        public Integer method(Integer param) {
            return param;
        }
        public Integer method2(Integer param) {
            return param;
        }
    }");
    
    writeFile($TestsPath."/Test_ClassRemovedSuperInterface.java",
    "import $PackageName.*;
    public class Test_ClassRemovedSuperInterface
    {
        public static void main(String[] args)
        {
            ClassRemovedSuperInterface Obj = new ClassRemovedSuperInterface();
            Integer Res = Obj.method2(0);
        }
    }");
    
    # Interface_Added_Super_Interface
    writeFile($Path_v1."/InterfaceAddedSuperInterface.java",
    "package $PackageName;
    public interface InterfaceAddedSuperInterface extends BaseInterface {
        public Integer someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceAddedSuperInterface.java",
    "package $PackageName;
    public interface InterfaceAddedSuperInterface extends BaseInterface, BaseInterface2 {
        public Integer someMethod(Integer param);
    }");
    
    # Interface_Added_Super_Constant_Interface
    writeFile($Path_v1."/InterfaceAddedSuperConstantInterface.java",
    "package $PackageName;
    public interface InterfaceAddedSuperConstantInterface extends BaseInterface {
        public Integer someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceAddedSuperConstantInterface.java",
    "package $PackageName;
    public interface InterfaceAddedSuperConstantInterface extends BaseInterface, BaseConstantInterface {
        public Integer someMethod(Integer param);
    }");
    
    # Interface_Removed_Super_Interface
    writeFile($Path_v1."/InterfaceRemovedSuperInterface.java",
    "package $PackageName;
    public interface InterfaceRemovedSuperInterface extends BaseInterface, BaseInterface2 {
        public Integer someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceRemovedSuperInterface.java",
    "package $PackageName;
    public interface InterfaceRemovedSuperInterface extends BaseInterface {
        public Integer someMethod(Integer param);
    }");
    
    # Interface_Removed_Super_Constant_Interface
    writeFile($Path_v1."/InterfaceRemovedSuperConstantInterface.java",
    "package $PackageName;
    public interface InterfaceRemovedSuperConstantInterface extends BaseInterface, BaseConstantInterface {
        public Integer someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceRemovedSuperConstantInterface.java",
    "package $PackageName;
    public interface InterfaceRemovedSuperConstantInterface extends BaseInterface {
        public Integer someMethod(Integer param);
    }");
    
    # Interface_Became_Class
    writeFile($Path_v1."/InterfaceBecameClass.java",
    "package $PackageName;
    public interface InterfaceBecameClass extends BaseInterface, BaseInterface2 {
        public Integer someMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceBecameClass.java",
    "package $PackageName;
    public class InterfaceBecameClass extends BaseClass {
        public Integer someMethod(Integer param) {
            return param;
        }
    }");
    
    # Removed_Class
    writeFile($Path_v1."/RemovedClass.java",
    "package $PackageName;
    public class RemovedClass extends BaseClass {
        public Integer someMethod(Integer param){
            return param;
        }
    }");
    
    # Removed_Class (w/o methods)
    writeFile($Path_v1."/RemovedFieldClass.java",
    "package $PackageName;
    public class RemovedFieldClass {
        public Integer field;
    }");
    
    writeFile($TestsPath."/Test_RemovedFieldClass.java",
    "import $PackageName.*;
    public class Test_RemovedFieldClass extends RemovedFieldClass
    {
        public static void main(String[] args)
        {
            RemovedFieldClass X = new RemovedFieldClass();
            Integer Copy = X.field;
        }
    }");
    
    # Removed_Class (with static fields, private constructor)
    writeFile($Path_v1."/RemovedClassWithStaticField.java",
    "package $PackageName;
    public class RemovedClassWithStaticField
    {
        private RemovedClassWithStaticField(){}
        public static Integer cnt = 0;
    }");
    
    writeFile($TestsPath."/Test_RemovedClassWithStaticField.java",
    "import $PackageName.*;
    public class Test_RemovedClassWithStaticField
    {
        public static void main(String[] args)
        {
            Integer Copy = RemovedClassWithStaticField.cnt;
        }
    }");
    
    # Removed_Field (static field, private constructor)
    writeFile($Path_v1."/RemovedStaticFieldFromClassWithPrivateCtor.java",
    "package $PackageName;
    public class RemovedStaticFieldFromClassWithPrivateCtor
    {
        private RemovedStaticFieldFromClassWithPrivateCtor(){}
        public static Integer cnt = 0;
    }");
    
    writeFile($Path_v2."/RemovedStaticFieldFromClassWithPrivateCtor.java",
    "package $PackageName;
    public class RemovedStaticFieldFromClassWithPrivateCtor
    {
        private RemovedStaticFieldFromClassWithPrivateCtor(){}
    }");
    
    writeFile($TestsPath."/Test_RemovedStaticFieldFromClassWithPrivateCtor.java",
    "import $PackageName.*;
    public class Test_RemovedStaticFieldFromClassWithPrivateCtor
    {
        public static void main(String[] args)
        {
            Integer Copy = RemovedStaticFieldFromClassWithPrivateCtor.cnt;
        }
    }");
    
    # Removed_Constant_Field
    writeFile($Path_v1."/ClassRemovedStaticConstantField.java",
    "package $PackageName;
    public class ClassRemovedStaticConstantField
    {
        public static int removedField_Int = 1000;
        public static String removedField_Str = \"Value\";
    }");
    writeFile($Path_v2."/ClassRemovedStaticConstantField.java",
    "package $PackageName;
    public class ClassRemovedStaticConstantField {
    }");
    
    writeFile($TestsPath."/Test_ClassRemovedStaticConstantField.java",
    "import $PackageName.*;
    public class Test_ClassRemovedStaticConstantField
    {
        public static void main(String[] args)
        {
            Integer Copy = ClassRemovedStaticConstantField.removedField_Int;
        }
    }");
    
    # Removed_Class (Deprecated)
    writeFile($Path_v1."/RemovedDeprecatedClass.java",
    "package $PackageName;
    \@Deprecated
    public class RemovedDeprecatedClass {
        public Integer someMethod(Integer param){
            return param;
        }
    }");
    
    # Removed_Interface
    writeFile($Path_v1."/RemovedInterface.java",
    "package $PackageName;
    public interface RemovedInterface extends BaseInterface, BaseInterface2 {
        public Integer someMethod(Integer param);
    }");
    
    # NonAbstract_Class_Added_Abstract_Method
    writeFile($Path_v1."/NonAbstractClassAddedAbstractMethod.java",
    "package $PackageName;
    public class NonAbstractClassAddedAbstractMethod {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
    }");
    writeFile($Path_v2."/NonAbstractClassAddedAbstractMethod.java",
    "package $PackageName;
    public abstract class NonAbstractClassAddedAbstractMethod {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public abstract Integer addedMethod(Integer param);
    }");
    
    # Abstract_Class_Added_Abstract_Method
    writeFile($Path_v1."/AbstractClassAddedAbstractMethod.java",
    "package $PackageName;
    public abstract class AbstractClassAddedAbstractMethod {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
    }");
    writeFile($Path_v2."/AbstractClassAddedAbstractMethod.java",
    "package $PackageName;
    public abstract class AbstractClassAddedAbstractMethod {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public abstract Integer addedMethod(Integer param);
    }");
    
    # Class_Became_Abstract
    writeFile($Path_v1."/ClassBecameAbstract.java",
    "package $PackageName;
    public class ClassBecameAbstract {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
    }");
    writeFile($Path_v2."/ClassBecameAbstract.java",
    "package $PackageName;
    public abstract class ClassBecameAbstract {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public abstract Integer addedMethod(Integer param);
    }");
    
    # Class_Became_Final
    writeFile($Path_v1."/ClassBecameFinal.java",
    "package $PackageName;
    public class ClassBecameFinal {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
    }");
    writeFile($Path_v2."/ClassBecameFinal.java",
    "package $PackageName;
    public final class ClassBecameFinal {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
    }");
    
    # Class_Removed_Abstract_Method
    writeFile($Path_v1."/ClassRemovedAbstractMethod.java",
    "package $PackageName;
    public abstract class ClassRemovedAbstractMethod {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public abstract Integer removedMethod(Integer param);
    }");
    writeFile($Path_v2."/ClassRemovedAbstractMethod.java",
    "package $PackageName;
    public abstract class ClassRemovedAbstractMethod {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
    }");
    
    # Class_Method_Became_Abstract
    writeFile($Path_v1."/ClassMethodBecameAbstract.java",
    "package $PackageName;
    public abstract class ClassMethodBecameAbstract {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public Integer someMethod(Integer param){
            return param;
        };
    }");
    writeFile($Path_v2."/ClassMethodBecameAbstract.java",
    "package $PackageName;
    public abstract class ClassMethodBecameAbstract {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public abstract Integer someMethod(Integer param);
    }");
    
    # Class_Method_Became_NonAbstract
    writeFile($Path_v1."/ClassMethodBecameNonAbstract.java",
    "package $PackageName;
    public abstract class ClassMethodBecameNonAbstract {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public abstract Integer someMethod(Integer param);
    }");
    writeFile($Path_v2."/ClassMethodBecameNonAbstract.java",
    "package $PackageName;
    public abstract class ClassMethodBecameNonAbstract {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
        public Integer someMethod(Integer param){
            return param;
        };
    }");
    
    # Method_Became_Static
    writeFile($Path_v1."/MethodBecameStatic.java",
    "package $PackageName;
    public class MethodBecameStatic {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/MethodBecameStatic.java",
    "package $PackageName;
    public class MethodBecameStatic {
        public static Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # Method_Became_NonStatic
    writeFile($Path_v1."/MethodBecameNonStatic.java",
    "package $PackageName;
    public class MethodBecameNonStatic {
        public static Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/MethodBecameNonStatic.java",
    "package $PackageName;
    public class MethodBecameNonStatic {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # Static_Method_Became_Final
    writeFile($Path_v1."/StaticMethodBecameFinal.java",
    "package $PackageName;
    public class StaticMethodBecameFinal {
        public static Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/StaticMethodBecameFinal.java",
    "package $PackageName;
    public class StaticMethodBecameFinal {
        public static final Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # NonStatic_Method_Became_Final
    writeFile($Path_v1."/NonStaticMethodBecameFinal.java",
    "package $PackageName;
    public class NonStaticMethodBecameFinal {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/NonStaticMethodBecameFinal.java",
    "package $PackageName;
    public class NonStaticMethodBecameFinal {
        public final Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # Method_Became_Abstract
    writeFile($Path_v1."/MethodBecameAbstract.java",
    "package $PackageName;
    public abstract class MethodBecameAbstract {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/MethodBecameAbstract.java",
    "package $PackageName;
    public abstract class MethodBecameAbstract {
        public abstract Integer someMethod(Integer param);
    }");
    
    # Method_Became_NonAbstract
    writeFile($Path_v1."/MethodBecameNonAbstract.java",
    "package $PackageName;
    public abstract class MethodBecameNonAbstract {
        public abstract Integer someMethod(Integer param);
    }");
    writeFile($Path_v2."/MethodBecameNonAbstract.java",
    "package $PackageName;
    public abstract class MethodBecameNonAbstract {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # Changed_Method_Access
    writeFile($Path_v1."/ChangedMethodAccess.java",
    "package $PackageName;
    public class ChangedMethodAccess {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/ChangedMethodAccess.java",
    "package $PackageName;
    public class ChangedMethodAccess {
        protected Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # Method_Became_Synchronized
    writeFile($Path_v1."/MethodBecameSynchronized.java",
    "package $PackageName;
    public class MethodBecameSynchronized {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/MethodBecameSynchronized.java",
    "package $PackageName;
    public class MethodBecameSynchronized {
        public synchronized Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # Method_Became_NonSynchronized
    writeFile($Path_v1."/MethodBecameNonSynchronized.java",
    "package $PackageName;
    public class MethodBecameNonSynchronized {
        public synchronized Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v2."/MethodBecameNonSynchronized.java",
    "package $PackageName;
    public class MethodBecameNonSynchronized {
        public Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    # Class_Overridden_Method
    writeFile($Path_v1."/OverriddenMethod.java",
    "package $PackageName;
    public class OverriddenMethod extends BaseClass {
        public Integer someMethod(Integer param) { return param; }
    }");
    writeFile($Path_v2."/OverriddenMethod.java",
    "package $PackageName;
    public class OverriddenMethod extends BaseClass {
        public Integer someMethod(Integer param) { return param; }
        public Integer method(Integer param) { return 2*param; }
    }");
    
    # Class_Method_Moved_Up_Hierarchy
    writeFile($Path_v1."/ClassMethodMovedUpHierarchy.java",
    "package $PackageName;
    public class ClassMethodMovedUpHierarchy extends BaseClass {
        public Integer someMethod(Integer param) { return param; }
        public Integer method(Integer param) { return 2*param; }
    }");
    writeFile($Path_v2."/ClassMethodMovedUpHierarchy.java",
    "package $PackageName;
    public class ClassMethodMovedUpHierarchy extends BaseClass {
        public Integer someMethod(Integer param) { return param; }
    }");
    
    # Class_Method_Moved_Up_Hierarchy (Interface Method) - should not be reported
    writeFile($Path_v1."/InterfaceMethodMovedUpHierarchy.java",
    "package $PackageName;
    public interface InterfaceMethodMovedUpHierarchy extends BaseInterface {
        public Integer method(Integer param);
        public Integer method2(Integer param);
    }");
    writeFile($Path_v2."/InterfaceMethodMovedUpHierarchy.java",
    "package $PackageName;
    public interface InterfaceMethodMovedUpHierarchy extends BaseInterface {
        public Integer method2(Integer param);
    }");
    
    # Class_Method_Moved_Up_Hierarchy (Abstract Method) - should not be reported
    writeFile($Path_v1."/AbstractMethodMovedUpHierarchy.java",
    "package $PackageName;
    public abstract class AbstractMethodMovedUpHierarchy implements BaseInterface {
        public abstract Integer method(Integer param);
        public abstract Integer method2(Integer param);
    }");
    writeFile($Path_v2."/AbstractMethodMovedUpHierarchy.java",
    "package $PackageName;
    public abstract class AbstractMethodMovedUpHierarchy implements BaseInterface {
        public abstract Integer method2(Integer param);
    }");
    
    # Use
    writeFile($Path_v1."/Use.java",
    "package $PackageName;
    public class Use
    {
        public FieldBecameFinal field;
        public void someMethod(FieldBecameFinal[] param) { };
        public void someMethod(Use param) { };
        public Integer someMethod(AbstractClassAddedSuperAbstractClass param) {
            return 0;
        }
        public Integer someMethod(AbstractClassAddedAbstractMethod param) {
            return 0;
        }
        public Integer someMethod(InterfaceAddedAbstractMethod param) {
            return 0;
        }
        public Integer someMethod(InterfaceAddedSuperInterface param) {
            return 0;
        }
        public Integer someMethod(AbstractClassAddedSuperInterface param) {
            return 0;
        }
        public Integer someMethod(AbstractClassAddedSuperInterfaceWithImplementedMethods param) {
            return 0;
        }
    }");
    writeFile($Path_v2."/Use.java",
    "package $PackageName;
    public class Use
    {
        public FieldBecameFinal field;
        public void someMethod(FieldBecameFinal[] param) { };
        public void someMethod(Use param) { };
        public Integer someMethod(AbstractClassAddedSuperAbstractClass param) {
            return param.abstractMethod(100)+param.field;
        }
        public Integer someMethod(AbstractClassAddedAbstractMethod param) {
            return param.addedMethod(100);
        }
        public Integer someMethod(InterfaceAddedAbstractMethod param) {
            return param.addedMethod(100);
        }
        public Integer someMethod(InterfaceAddedSuperInterface param) {
            return param.method2(100);
        }
        public Integer someMethod(AbstractClassAddedSuperInterface param) {
            return param.method2(100);
        }
        public Integer someMethod(AbstractClassAddedSuperInterfaceWithImplementedMethods param) {
            return param.method2(100);
        }
    }");
    
    # Added_Package
    writeFile($Path_v2."/AddedPackage/AddedPackageClass.java",
    "package $PackageName.AddedPackage;
    public class AddedPackageClass {
        public Integer field;
        public void someMethod(Integer param) { };
    }");
    
    # Removed_Package
    writeFile($Path_v1."/RemovedPackage/RemovedPackageClass.java",
    "package $PackageName.RemovedPackage;
    public class RemovedPackageClass {
        public Integer field;
        public void someMethod(Integer param) { };
    }");
    my $BuildRoot1 = get_dirname($Path_v1);
    my $BuildRoot2 = get_dirname($Path_v2);
    if(compileJavaLib($LibName, $BuildRoot1, $BuildRoot2))
    {
        runTests($TestsPath, $PackageName, $BuildRoot1, $BuildRoot2);
        runChecker($LibName, $BuildRoot1, $BuildRoot2);
    }
}

sub readArchive($$)
{ # 1, 2 - library, 0 - client
    my ($LibVersion, $Path) = @_;
    return if(not $Path or not -e $Path);
    
    if($LibVersion)
    {
        my $ArchiveName = get_filename($Path);
        $LibArchives{$LibVersion}{$ArchiveName} = 1;
    }
    
    $Path = get_abs_path($Path);
    my $JarCmd = get_CmdPath("jar");
    if(not $JarCmd) {
        exitStatus("Not_Found", "can't find \"jar\" command");
    }
    my $ExtractPath = joinPath($TMP_DIR, $ExtractCounter);
    if(-d $ExtractPath) {
        rmtree($ExtractPath);
    }
    mkpath($ExtractPath);
    chdir($ExtractPath);
    system($JarCmd." -xf \"$Path\"");
    if($?) {
        exitStatus("Error", "can't extract \'$Path\'");
    }
    chdir($ORIG_DIR);
    my @Classes = ();
    foreach my $ClassPath (cmd_find($ExtractPath,"","*\.class",""))
    {
        if($OSgroup ne "windows") {
            $ClassPath=~s/\.class\Z//g;
        }
        
        my $ClassName = get_filename($ClassPath);
        if($ClassName=~/\$\d/) {
            next;
        }
        $ClassPath = cut_path_prefix($ClassPath, $ExtractPath); # javap decompiler accepts relative paths only
        
        my $ClassDir = get_dirname($ClassPath);
        if($ClassDir=~/\./)
        { # jaxb-osgi.jar/1.0/org/apache
            next;
        }
        
        my $Package = get_PFormat($ClassDir);
        if($LibVersion)
        {
            if(skipPackage($Package, $LibVersion))
            { # internal packages
                next;
            }
        }
        
        $ClassName=~s/\$/./g; # real name for GlyphView$GlyphPainter is GlyphView.GlyphPainter
        push(@Classes, $ClassPath);
    }
    
    if($#Classes!=-1)
    {
        foreach my $PartRef (divideArray(\@Classes))
        {
            if($LibVersion) {
                readClasses($PartRef, $LibVersion, get_filename($Path));
            }
            else {
                readClasses_Usage($PartRef);
            }
        }
    }
    
    $ExtractCounter+=1;
    
    if($LibVersion)
    {
        foreach my $SubArchive (cmd_find($ExtractPath,"","*\.jar",""))
        { # recursive step
            readArchive($LibVersion, $SubArchive);
        }
    }
    
    rmtree($ExtractPath);
}

sub native_path($)
{
    my $Path = $_[0];
    if($OSgroup eq "windows") {
        $Path=~s/[\/\\]+/\\/g;
    }
    return $Path;
}

sub divideArray($)
{
    my $ArrRef = $_[0];
    return () if(not $ArrRef);
    my @Array = @{$ArrRef};
    return () if($#{$ArrRef}==-1);
    
    my @Res = ();
    my $Sub = [];
    my $Len = 0;
    
    foreach my $Pos (0 .. $#{$ArrRef})
    {
        my $Arg = $ArrRef->[$Pos];
        my $Arg_L = length($Arg) + 1; # space
        if($Len < $ARG_MAX - 250)
        {
            push(@{$Sub}, $Arg);
            $Len += $Arg_L;
        }
        else
        {
            push(@Res, $Sub);
            
            $Sub = [$Arg];
            $Len = $Arg_L;
        }
    }
    
    if($#{$Sub}!=-1) {
        push(@Res, $Sub);
    }
    
    return @Res;
}

sub registerType($$)
{
    my ($TName, $LibVersion) = @_;
    
    if(not $TName) {
        return 0;
    }
    
    $TName=~s/#/./g;
    if($TName_Tid{$LibVersion}{$TName}) {
        return $TName_Tid{$LibVersion}{$TName};
    }
    
    if(not $TName_Tid{$LibVersion}{$TName})
    {
        my $ID = undef;
        if($REPRODUCIBLE) {
            $ID = getMd5($TName);
        }
        else {
            $ID = ++$TYPE_ID;
        }
        $TName_Tid{$LibVersion}{$TName} = "$ID";
    }
    
    my $Tid = $TName_Tid{$LibVersion}{$TName};
    $TypeInfo{$LibVersion}{$Tid}{"Name"} = $TName;
    if($TName=~/(.+)\[\]\Z/)
    {
        if(my $BaseTypeId = registerType($1, $LibVersion))
        {
            $TypeInfo{$LibVersion}{$Tid}{"BaseType"} = $BaseTypeId;
            $TypeInfo{$LibVersion}{$Tid}{"Type"} = "array";
        }
    }
    
    return $Tid;
}

sub readClasses_Usage($)
{
    my $Paths = $_[0];
    return () if(not $Paths);
    
    my $JavapCmd = get_CmdPath("javap");
    if(not $JavapCmd) {
        exitStatus("Not_Found", "can't find \"javap\" command");
    }
    
    my $Input = join(" ", @{$Paths});
    if($OSgroup ne "windows")
    { # on unix ensure that the system does not try and interpret the $, by escaping it
        $Input=~s/\$/\\\$/g;
    }
    
    chdir($TMP_DIR."/".$ExtractCounter);
    open(CONTENT, "$JavapCmd -c -private $Input 2>\"$TMP_DIR/warn\" |");
    while(<CONTENT>)
    {
        if(/\/\/\s*(Method|InterfaceMethod)\s+(.+)\Z/)
        {
            my $M = $2;
            $UsedMethods_Client{$M} = 1;
            
            if($M=~/\A(.*)+\.\w+\:\(/)
            {
                my $C = $1;
                $C=~s/\//./g;
                $UsedClasses_Client{$C} = 1;
            }
        }
        elsif(/\/\/\s*Field\s+(.+)\Z/)
        {
            my $FieldName = $1;
            if(/\s+(putfield|getfield|getstatic|putstatic)\s+/) {
                $UsedFields_Client{$FieldName} = $1;
            }
        }
        elsif(/ ([^\s]+) [^: ]+\(([^()]+)\)/)
        {
            my ($Ret, $Params) = ($1, $2);
            
            $Ret=~s/\[\]//g; # quals
            $UsedClasses_Client{$Ret} = 1;
            
            foreach my $Param (split(/\s*,\s*/, $Params))
            {
                $Param=~s/\[\]//g; # quals
                $UsedClasses_Client{$Param} = 1;
            }
        }
        elsif(/ class /)
        {
            if(/extends ([^\s{]+)/)
            {
                foreach my $Class (split(/\s*,\s*/, $1)) {
                    $UsedClasses_Client{$Class} = 1;
                }
            }
            
            if(/implements ([^\s{]+)/)
            {
                foreach my $Interface (split(/\s*,\s*/, $1)) {
                    $UsedClasses_Client{$Interface} = 1;
                }
            }
        }
    }
    close(CONTENT);
    chdir($ORIG_DIR);
}

sub readClasses($$$)
{
    my ($Paths, $LibVersion, $ArchiveName) = @_;
    return if(not $Paths or not $LibVersion or not $ArchiveName);
    
    my $JavapCmd = get_CmdPath("javap");
    if(not $JavapCmd) {
        exitStatus("Not_Found", "can't find \"javap\" command");
    }
    
    my $Input = join(" ", @{$Paths});
    if($OSgroup ne "windows")
    { # on unix ensure that the system does not try and interpret the $, by escaping it
        $Input=~s/\$/\\\$/g;
    }
    
    my $Output = $TMP_DIR."/class-dump.txt";
    if(-e $Output) {
        unlink($Output);
    }
    
    my $Cmd = "$JavapCmd -s -private";
    if(not $Quick) {
        $Cmd .= " -c -verbose";
    }
    
    chdir($TMP_DIR."/".$ExtractCounter);
    system($Cmd." ".$Input." >\"$Output\" 2>\"$TMP_DIR/warn\"");
    chdir($ORIG_DIR);
    
    if(not -e $Output) {
        exitStatus("Error", "internal error in parser, try to reduce ARG_MAX");
    }
    if($Debug) {
        appendFile($DEBUG_PATH{$LibVersion}."/class-dump.txt", readFile($Output));
    }
    
    # ! private info should be processed
    open(CONTENT, "$TMP_DIR/class-dump.txt");
    my @Content = <CONTENT>;
    close(CONTENT);
    
    my (%TypeAttr, $CurrentMethod, $CurrentPackage, $CurrentClass) = ();
    my ($InParamTable, $InExceptionTable, $InCode) = (0, 0, 0);
    
    my $InAnnotations = undef;
    my $InAnnotations_Class = undef;
    my $InAnnotations_Method = undef;
    my %AnnotationName = ();
    my %AnnotationNum = (); # support for Java 7
    
    my ($ParamPos, $FieldPos, $LineNum) = (0, 0, 0);
    while($LineNum<=$#Content)
    {
        my $LINE = $Content[$LineNum++];
        my $LINE_N = $Content[$LineNum];
        
        if($LINE=~/\A\s*(?:const|AnnotationDefault|Compiled|Source|Constant)/) {
            next;
        }
        
        if($LINE=~/\sof\s|\sline \d+:|\[\s*class|= \[|\$[\d\$\(:\.;]| class\$|[\.\/]\$|\._\d|\$eq/)
        { # artificial methods and code
            next;
        }
        
        if($LINE=~/ (\w+\$|)\w+\$\w+[\(:]/) {
            next;
        }
        
        # $LINE=~s/ \$(\w)/ $1/g;
        # $LINE_N=~s/ \$(\w)/ $1/g;
        
        if(not $InParamTable)
        {
            if($LINE=~/ \$/) {
                next;
            }
        }
        
        $LINE=~s/\$([\> ]|\Z)/$1/g;
        $LINE_N=~s/\$([\> ]|\Z)/$1/g;
        
        if($LINE eq "\n" or $LINE eq "}\n")
        {
            $CurrentMethod = undef;
            $InCode = 0;
            $InAnnotations_Method = 0;
            $InParamTable = 0;
        }
        
        if($LINE eq "}\n") {
            $InAnnotations_Class = 1;
        }
        
        if($LINE=~/\A\s*#(\d+)/)
        { # Constant pool
            my $CNum = $1;
            if($LINE=~/\s+([^ ]+?);/)
            {
                my $AName = $1;
                $AName=~s/\AL//;
                $AName=~s/\$/./g;
                $AName=~s/\//./g;
                
                $AnnotationName{$CNum} = $AName;
                
                if(defined $AnnotationNum{$CNum})
                { # support for Java 7
                    if($InAnnotations_Class) {
                        $TypeAttr{"Annotations"}{registerType($AName, $LibVersion)} = 1;
                    }
                    delete($AnnotationNum{$CNum});
                }
            }
            
            next;
        }
        
        # Java 7: templates
        if(index($LINE, "<")!=-1)
        { # <T extends java.lang.Object>
          # <KEYIN extends java.lang.Object ...
            if($LINE=~/<[A-Z\d\?]+ /i)
            {
                while($LINE=~/<([A-Z\d\?]+ .*?)>( |\Z)/i)
                {
                    my $Str = $1;
                    my @Prms = ();
                    foreach my $P (separate_Params($Str, 0, 0))
                    {
                        $P=~s/\A([A-Z\d\?]+) .*\Z/$1/ig;
                        push(@Prms, $P);
                    }
                    my $Str_N = join(", ", @Prms);
                    $LINE=~s/\Q$Str\E/$Str_N/g;
                }
            }
        }
        
        $LINE=~s/\s*,\s*/,/g;
        $LINE=~s/\$/#/g;
        
        if(index($LINE, "LocalVariableTable")!=-1) {
            $InParamTable += 1;
        }
        elsif($LINE=~/Exception\s+table/) {
            $InExceptionTable = 1;
        }
        elsif($LINE=~/\A\s*Code:/)
        {
            $InCode += 1;
            $InAnnotations = undef;
        }
        elsif($LINE=~/\A\s*\d+:\s*(.*)\Z/)
        { # read Code
            if($InCode==1)
            {
                if($CurrentMethod)
                {
                    if(index($LINE, "invoke")!=-1)
                    {
                        if($LINE=~/ invoke(\w+) .* \/\/\s*(Method|InterfaceMethod)\s+(.+)\Z/)
                        { # 3:   invokevirtual   #2; //Method "[Lcom/sleepycat/je/Database#DbState;".clone:()Ljava/lang/Object;
                            my ($InvokeType, $InvokedName) = ($1, $3);
                            
                            if($InvokedName!~/\A(\w+:|java\/(lang|util|io)\/)/
                            and index($InvokedName, '"<init>":')!=0)
                            {
                                $InvokedName=~s/#/\$/g;
                                $MethodUsed{$LibVersion}{$InvokedName}{$CurrentMethod} = $InvokeType;
                            }
                        }
                    }
                    # elsif($LINE=~/ (getstatic|putstatic) .* \/\/\s*Field\s+(.+)\Z/)
                    # {
                    #     my $UsedFieldName = $2;
                    #     $FieldUsed{$LibVersion}{$UsedFieldName}{$CurrentMethod} = 1;
                    # }
                }
            }
            elsif(defined $InAnnotations)
            {
                if($LINE=~/\A\s*\d+\:\s*#(\d+)/)
                {
                    if(my $AName = $AnnotationName{$1})
                    {
                        if($InAnnotations_Class) {
                            $TypeAttr{"Annotations"}{registerType($AName, $LibVersion)} = 1;
                        }
                        elsif($InAnnotations_Method) {
                            $MethodInfo{$LibVersion}{$CurrentMethod}{"Annotations"}{registerType($AName, $LibVersion)} = 1;
                        }
                    }
                    else
                    { # suport for Java 7
                        $AnnotationNum{$1} = 1;
                    }
                }
            }
        }
        elsif($CurrentMethod and $InParamTable==1 and $LINE=~/\A\s+0\s+\d+\s+\d+\s+(\#?)(\w+)/)
        { # read parameter names from LocalVariableTable
            my $Art = $1;
            my $PName = $2;
            
            if(($PName ne "this" or $Art) and $PName=~/[a-z]/i)
            {
                if($CurrentMethod)
                {
                    if(defined $MethodInfo{$LibVersion}{$CurrentMethod}
                    and defined $MethodInfo{$LibVersion}{$CurrentMethod}{"Param"}
                    and defined $MethodInfo{$LibVersion}{$CurrentMethod}{"Param"}{$ParamPos}
                    and defined $MethodInfo{$LibVersion}{$CurrentMethod}{"Param"}{$ParamPos}{"Type"})
                    {
                        $MethodInfo{$LibVersion}{$CurrentMethod}{"Param"}{$ParamPos}{"Name"} = $PName;
                        $ParamPos++;
                    }
                }
            }
        }
        elsif($CurrentClass and $LINE=~/(\A|\s+)([^\s]+)\s+([^\s]+)\s*\((.*)\)\s*(throws\s*([^\s]+)|)\s*;\Z/)
        { # attributes of methods and constructors
            my (%MethodAttr, $ParamsLine, $Exceptions) = ();
            
            $InParamTable = 0; # read the first local variable table
            $InCode = 0; # read the first code
            $InAnnotations_Method = 1;
            $InAnnotations_Class = 0;
            
            ($MethodAttr{"Return"}, $MethodAttr{"ShortName"}, $ParamsLine, $Exceptions) = ($2, $3, $4, $6);
            $MethodAttr{"ShortName"}=~s/#/./g;
            
            if($Exceptions)
            {
                foreach my $E (split(/,/, $Exceptions)) {
                    $MethodAttr{"Exceptions"}{registerType($E, $LibVersion)} = 1;
                }
            }
            if($LINE=~/(\A|\s+)(public|protected|private)\s+/) {
                $MethodAttr{"Access"} = $2;
            }
            else {
                $MethodAttr{"Access"} = "package-private";
            }
            $MethodAttr{"Class"} = registerType($TypeAttr{"Name"}, $LibVersion);
            if($MethodAttr{"ShortName"}=~/\A(|(.+)\.)\Q$CurrentClass\E\Z/)
            {
                if($2)
                {
                    $MethodAttr{"Package"} = $2;
                    $CurrentPackage = $MethodAttr{"Package"};
                    $MethodAttr{"ShortName"} = $CurrentClass;
                }
                $MethodAttr{"Constructor"} = 1;
                delete($MethodAttr{"Return"});
            }
            else
            {
                $MethodAttr{"Return"} = registerType($MethodAttr{"Return"}, $LibVersion);
            }
            
            my @Params = separate_Params($ParamsLine, 0, 1);
            
            $ParamPos = 0;
            foreach my $ParamTName (@Params)
            {
                %{$MethodAttr{"Param"}{$ParamPos}} = ("Type"=>registerType($ParamTName, $LibVersion), "Name"=>"p".($ParamPos+1));
                $ParamPos++;
            }
            $ParamPos = 0;
            if(not $MethodAttr{"Constructor"})
            { # methods
                if($CurrentPackage) {
                    $MethodAttr{"Package"} = $CurrentPackage;
                }
                if($LINE=~/(\A|\s+)abstract\s+/) {
                    $MethodAttr{"Abstract"} = 1;
                }
                if($LINE=~/(\A|\s+)final\s+/) {
                    $MethodAttr{"Final"} = 1;
                }
                if($LINE=~/(\A|\s+)static\s+/) {
                    $MethodAttr{"Static"} = 1;
                }
                if($LINE=~/(\A|\s+)native\s+/) {
                    $MethodAttr{"Native"} = 1;
                }
                if($LINE=~/(\A|\s+)synchronized\s+/) {
                    $MethodAttr{"Synchronized"} = 1;
                }
            }
            
            # read the Signature
            if($LINE_N=~/(Signature|descriptor):\s*(.+)\Z/i)
            { # create run-time unique name ( java/io/PrintStream.println (Ljava/lang/String;)V )
                if($MethodAttr{"Constructor"}) {
                    $CurrentMethod = $CurrentClass.".\"<init>\":".$2;
                }
                else {
                    $CurrentMethod = $CurrentClass.".".$MethodAttr{"ShortName"}.":".$2;
                }
                if(my $PackageName = get_SFormat($CurrentPackage)) {
                    $CurrentMethod = $PackageName."/".$CurrentMethod;
                }
                
                $LineNum++;
            }
            else {
                exitStatus("Error", "internal error - can't read method signature");
            }
            
            $MethodAttr{"Archive"} = $ArchiveName;
            if($CurrentMethod)
            {
                %{$MethodInfo{$LibVersion}{$CurrentMethod}} = %MethodAttr;
                if($MethodAttr{"Access"}=~/public|protected/)
                {
                    $Class_Methods{$LibVersion}{$TypeAttr{"Name"}}{$CurrentMethod} = 1;
                    if($MethodAttr{"Abstract"}) {
                        $Class_AbstractMethods{$LibVersion}{$TypeAttr{"Name"}}{$CurrentMethod} = 1;
                    }
                }
            }
        }
        elsif($CurrentClass and $LINE=~/(\A|\s+)([^\s]+)\s+(\w+);\Z/)
        { # fields
            my ($TName, $FName) = ($2, $3);
            $TypeAttr{"Fields"}{$FName}{"Type"} = registerType($TName, $LibVersion);
            if($LINE=~/(\A|\s+)final\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Final"} = 1;
            }
            if($LINE=~/(\A|\s+)static\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Static"} = 1;
            }
            if($LINE=~/(\A|\s+)transient\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Transient"} = 1;
            }
            if($LINE=~/(\A|\s+)volatile\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Volatile"} = 1;
            }
            if($LINE=~/(\A|\s+)(public|protected|private)\s+/) {
                $TypeAttr{"Fields"}{$FName}{"Access"} = $2;
            }
            else {
                $TypeAttr{"Fields"}{$FName}{"Access"} = "package-private";
            }
            if($TypeAttr{"Fields"}{$FName}{"Access"}!~/private/) {
                $Class_Fields{$LibVersion}{$TypeAttr{"Name"}}{$FName}=$TypeAttr{"Fields"}{$FName}{"Type"};
            }
            $TypeAttr{"Fields"}{$FName}{"Pos"} = $FieldPos++;
            
            # read the Signature
            if($Content[$LineNum++]=~/(Signature|descriptor):\s*(.+)\Z/i)
            {
                my $FSignature = $2;
                if(my $PackageName = get_SFormat($CurrentPackage)) {
                    $TypeAttr{"Fields"}{$FName}{"Mangled"} = $PackageName."/".$CurrentClass.".".$FName.":".$FSignature;
                }
            }
            if($Content[$LineNum]=~/flags:/i)
            { # flags: ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_ANNOTATION
                $LineNum++;
            }
            
            # read the Value
            if($Content[$LineNum]=~/Constant\s*value:\s*([^\s]+)\s(.*)\Z/i)
            {
              # Java 6: Constant value: ...
              # Java 7: ConstantValue: ...
                $LineNum+=1;
                my ($TName, $Value) = ($1, $2);
                if($Value)
                {
                    if($Value=~s/Deprecated:\s*true\Z//g) {
                        # deprecated values: ?
                    }
                    $TypeAttr{"Fields"}{$FName}{"Value"} = $Value;
                }
                elsif($TName eq "String") {
                    $TypeAttr{"Fields"}{$FName}{"Value"} = "\@EMPTY_STRING\@";
                }
            }
        }
        elsif($LINE=~/(\A|\s+)(class|interface)\s+([^\s\{]+)(\s+|\{|\Z)/)
        { # properties of classes and interfaces
            if($TypeAttr{"Name"})
            { # register previous
                %{$TypeInfo{$LibVersion}{registerType($TypeAttr{"Name"}, $LibVersion)}} = %TypeAttr;
            }
            
            %TypeAttr = ("Type"=>$2, "Name"=>$3); # reset previous class
            %AnnotationName = (); # reset annotations of the class
            %AnnotationNum = (); # support for Java 7
            $InAnnotations_Class = 1;
            
            $FieldPos = 0; # reset field position
            $CurrentMethod = ""; # reset current method
            $TypeAttr{"Archive"} = $ArchiveName;
            if($TypeAttr{"Name"}=~/\A(.+)\.([^.]+)\Z/)
            {
                $CurrentClass = $2;
                $TypeAttr{"Package"} = $1;
                $CurrentPackage = $TypeAttr{"Package"};
            }
            else
            {
                $CurrentClass = $TypeAttr{"Name"};
                $CurrentPackage = "";
            }
            if($CurrentClass=~s/#/./g)
            { # javax.swing.text.GlyphView.GlyphPainter <=> GlyphView$GlyphPainter
                $TypeAttr{"Name"}=~s/#/./g;
            }
            if($LINE=~/(\A|\s+)(public|protected|private)\s+/) {
                $TypeAttr{"Access"} = $2;
            }
            else {
                $TypeAttr{"Access"} = "package-private";
            }
            if($LINE=~/\s+extends\s+([^\s\{]+)/)
            {
                my $Extended = $1;
                
                if($TypeAttr{"Type"} eq "class")
                {
                    if($Extended ne $CurrentPackage.".".$CurrentClass) {
                        $TypeAttr{"SuperClass"} = registerType($Extended, $LibVersion);
                    }
                }
                elsif($TypeAttr{"Type"} eq "interface")
                {
                    my @Elems = separate_Params($Extended, 0, 0);
                    foreach my $SuperInterface (@Elems)
                    {
                        if($SuperInterface ne $CurrentPackage.".".$CurrentClass) {
                            $TypeAttr{"SuperInterface"}{registerType($SuperInterface, $LibVersion)} = 1;
                        }
                        
                        if($SuperInterface eq "java.lang.annotation.Annotation") {
                            $TypeAttr{"Annotation"} = 1;
                        }
                    }
                }
            }
            if($LINE=~/\s+implements\s+([^\s\{]+)/)
            {
                my $Implemented = $1;
                my @Elems = separate_Params($Implemented, 0, 0);
                
                foreach my $SuperInterface (@Elems) {
                    $TypeAttr{"SuperInterface"}{registerType($SuperInterface, $LibVersion)} = 1;
                }
            }
            if($LINE=~/(\A|\s+)abstract\s+/) {
                $TypeAttr{"Abstract"} = 1;
            }
            if($LINE=~/(\A|\s+)final\s+/) {
                $TypeAttr{"Final"} = 1;
            }
            if($LINE=~/(\A|\s+)static\s+/) {
                $TypeAttr{"Static"} = 1;
            }
        }
        elsif(index($LINE, "Deprecated: true")!=-1
        or index($LINE, "Deprecated: length")!=-1)
        { # deprecated method or class
            if($CurrentMethod) {
                $MethodInfo{$LibVersion}{$CurrentMethod}{"Deprecated"} = 1;
            }
            elsif($CurrentClass) {
                $TypeAttr{"Deprecated"} = 1;
            }
        }
        elsif(index($LINE, "RuntimeInvisibleAnnotations")!=-1
        or index($LINE, "RuntimeVisibleAnnotations")!=-1)
        {
            $InAnnotations = 1;
            $InCode = 0;
        }
        elsif(defined $InAnnotations and index($LINE, "InnerClasses")!=-1) {
            $InAnnotations = undef;
        }
        else
        {
            # unparsed
        }
    }
    if($TypeAttr{"Name"})
    { # register last
        %{$TypeInfo{$LibVersion}{registerType($TypeAttr{"Name"}, $LibVersion)}} = %TypeAttr;
    }
}

sub separate_Params($$$)
{
    my ($Params, $Comma, $Sp) = @_;
    my @Parts = ();
    my %B = ( "("=>0, "<"=>0, ")"=>0, ">"=>0 );
    my $Part = 0;
    foreach my $Pos (0 .. length($Params) - 1)
    {
        my $S = substr($Params, $Pos, 1);
        if(defined $B{$S}) {
            $B{$S} += 1;
        }
        if($S eq "," and
        $B{"("}==$B{")"} and $B{"<"}==$B{">"})
        {
            if($Comma)
            { # include comma
                $Parts[$Part] .= $S;
            }
            $Part += 1;
        }
        else {
            $Parts[$Part] .= $S;
        }
    }
    if(not $Sp)
    { # remove spaces
        foreach (@Parts)
        {
            s/\A //g;
            s/ \Z//g;
        }
    }
    return @Parts;
}

sub registerUsage($$)
{
    my ($TypeId, $LibVersion) = @_;
    $Class_Constructed{$LibVersion}{$TypeId} = 1;
    if(my $BaseId = $TypeInfo{$LibVersion}{$TypeId}{"BaseType"}) {
        $Class_Constructed{$LibVersion}{$BaseId} = 1;
    }
}

sub checkVoidMethod($)
{
    my $Method = $_[0];
    return "" if(not $Method);
    if($Method=~s/\)(.+)\Z/\)V/g) {
        return $Method;
    }
    else {
        return "";
    }
}

sub detectAdded()
{
    foreach my $Method (keys(%{$MethodInfo{2}}))
    {
        if(not defined $MethodInfo{1}{$Method})
        {
            if(not methodFilter($Method, 2)) {
                next;
            }
            
            my $ClassId = $MethodInfo{2}{$Method}{"Class"};
            my %Class = get_Type($ClassId, 2);
            
            $CheckedTypes{$Class{"Name"}} = 1;
            $CheckedMethods{$Method} = 1;
            
            if(not $MethodInfo{2}{$Method}{"Constructor"}
            and my $Overridden = findMethod($Method, 2, $Class{"Name"}, 2))
            {
                if(defined $MethodInfo{1}{$Overridden}
                and get_TypeType($ClassId, 2) eq "class" and $TName_Tid{1}{$Class{"Name"}})
                { # class should exist in previous version
                    %{$CompatProblems{$Overridden}{"Class_Overridden_Method"}{"this.".get_SFormat($Method)}}=(
                        "Type_Name"=>$Class{"Name"},
                        "Target"=>$MethodInfo{2}{$Method}{"Signature"},
                        "Old_Value"=>$Overridden,
                        "New_Value"=>$Method  );
                }
            }
            if($MethodInfo{2}{$Method}{"Abstract"}) {
                $AddedMethod_Abstract{$Class{"Name"}}{$Method} = 1;
            }
            
            %{$CompatProblems{$Method}{"Added_Method"}{""}}=();
            
            if(not $MethodInfo{2}{$Method}{"Constructor"})
            {
                if(get_TypeName($MethodInfo{2}{$Method}{"Return"}, 2) ne "void"
                and my $VoidMethod = checkVoidMethod($Method))
                {
                    if(defined $MethodInfo{1}{$VoidMethod})
                    { # return value type changed from "void" to 
                        $ChangedReturnFromVoid{$VoidMethod} = 1;
                        $ChangedReturnFromVoid{$Method} = 1;
                        
                        %{$CompatProblems{$VoidMethod}{"Changed_Method_Return_From_Void"}{""}}=(
                            "New_Value"=>get_TypeName($MethodInfo{2}{$Method}{"Return"}, 2)
                        );
                    }
                }
            }
        }
    }
}

sub detectRemoved()
{
    foreach my $Method (keys(%{$MethodInfo{1}}))
    {
        if(not defined $MethodInfo{2}{$Method})
        {
            if(not methodFilter($Method, 1)) {
                next;
            }
            
            my $ClassId = $MethodInfo{1}{$Method}{"Class"};
            my %Class = get_Type($ClassId, 1);
            
            $CheckedTypes{$Class{"Name"}} = 1;
            $CheckedMethods{$Method} = 1;
            
            if(not $MethodInfo{1}{$Method}{"Constructor"}
            and my $MovedUp = findMethod($Method, 1, $Class{"Name"}, 2))
            {
                if(get_TypeType($ClassId, 1) eq "class"
                and not $MethodInfo{1}{$Method}{"Abstract"} and $TName_Tid{2}{$Class{"Name"}})
                {# class should exist in newer version
                    %{$CompatProblems{$Method}{"Class_Method_Moved_Up_Hierarchy"}{"this.".get_SFormat($MovedUp)}}=(
                        "Type_Name"=>$Class{"Name"},
                        "Target"=>$MethodInfo{2}{$MovedUp}{"Signature"},
                        "Old_Value"=>$Method,
                        "New_Value"=>$MovedUp  );
                }
            }
            else
            {
                if($MethodInfo{1}{$Method}{"Abstract"}) {
                    $RemovedMethod_Abstract{$Class{"Name"}}{$Method} = 1;
                }
                %{$CompatProblems{$Method}{"Removed_Method"}{""}}=();
            }
        }
    }
}

sub getArchives($)
{
    my $LibVersion = $_[0];
    my @Paths = ();
    foreach my $Path (split(/\s*\n\s*/, $Descriptor{$LibVersion}{"Archives"}))
    {
        if(not -e $Path) {
            exitStatus("Access_Error", "can't access \'$Path\'");
        }
        foreach (getArchivePaths($Path, $LibVersion)) {
            push(@Paths, $_);
        }
    }
    return @Paths;
}

sub getArchivePaths($$)
{
    my ($Dest, $LibVersion) = @_;
    if(-f $Dest) {
        return ($Dest);
    }
    elsif(-d $Dest)
    {
        $Dest=~s/[\/\\]+\Z//g;
        my @AllClasses = ();
        foreach my $Path (cmd_find($Dest,"","*\.jar",""))
        {
            next if(ignore_path($Path, $Dest));
            push(@AllClasses, resolve_symlink($Path));
        }
        return @AllClasses;
    }
    return ();
}

sub isCyclical($$)
{
    return (grep {$_ eq $_[1]} @{$_[0]});
}

sub read_symlink($)
{
    my $Path = $_[0];
    return "" if(not $Path or not -f $Path);
    return $Cache{"read_symlink"}{$Path} if(defined $Cache{"read_symlink"}{$Path});
    if(my $ReadlinkCmd = get_CmdPath("readlink"))
    {
        my $Res = `$ReadlinkCmd -n \"$Path\"`;
        return ($Cache{"read_symlink"}{$Path} = $Res);
    }
    elsif(my $FileCmd = get_CmdPath("file"))
    {
        my $Info = `$FileCmd \"$Path\"`;
        if($Info=~/symbolic\s+link\s+to\s+['`"]*([\w\d\.\-\/\\]+)['`"]*/i) {
            return ($Cache{"read_symlink"}{$Path} = $1);
        }
    }
    return ($Cache{"read_symlink"}{$Path} = "");
}

sub resolve_symlink($)
{
    my $Path = $_[0];
    return "" if(not $Path or not -f $Path);
    return $Path if(isCyclical(\@RecurSymlink, $Path));
    push(@RecurSymlink, $Path);
    if(-l $Path and my $Redirect=read_symlink($Path))
    {
        if(is_abs($Redirect))
        {
            my $Res = resolve_symlink($Redirect);
            pop(@RecurSymlink);
            return $Res;
        }
        elsif($Redirect=~/\.\.[\/\\]/)
        {
            $Redirect = joinPath(get_dirname($Path),$Redirect);
            while($Redirect=~s&(/|\\)[^\/\\]+(\/|\\)\.\.(\/|\\)&$1&){};
            my $Res = resolve_symlink($Redirect);
            pop(@RecurSymlink);
            return $Res;
        }
        elsif(-f get_dirname($Path)."/".$Redirect)
        {
            my $Res = resolve_symlink(joinPath(get_dirname($Path),$Redirect));
            pop(@RecurSymlink);
            return $Res;
        }
        return $Path;
    }
    else
    {
        pop(@RecurSymlink);
        return $Path;
    }
}

sub cmpVersions($$)
{# compare two version strings in dotted-numeric format
    my ($V1, $V2) = @_;
    return 0 if($V1 eq $V2);
    my @V1Parts = split(/\./, $V1);
    my @V2Parts = split(/\./, $V2);
    for (my $i = 0; $i <= $#V1Parts && $i <= $#V2Parts; $i++) {
        return -1 if(int($V1Parts[$i]) < int($V2Parts[$i]));
        return 1 if(int($V1Parts[$i]) > int($V2Parts[$i]));
    }
    return -1 if($#V1Parts < $#V2Parts);
    return 1 if($#V1Parts > $#V2Parts);
    return 0;
}

sub majorVersion($)
{
    my $Version = $_[0];
    return 0 if(not $Version);
    my @VParts = split(/\./, $Version);
    return $VParts[0];
}

sub isDump($)
{
    if($_[0]=~/\A(.+)\.(api|dump|apidump)(\Q.tar.gz\E|\Q.zip\E|)\Z/)
    { # returns a name of package
        return $1;
    }
    return 0;
}

sub isDumpFile($)
{
    if($_[0]=~/\.(api|dump|apidump)\Z/)
    {
        return 1;
    }
    return 0;
}

sub read_API_Dump($$$)
{
    my ($LibVersion, $Path, $Subj) = @_;
    return if(not $LibVersion or not -e $Path);
    
    my $FilePath = unpackDump($Path);
    if(not isDumpFile($FilePath)) {
        exitStatus("Invalid_Dump", "specified API dump \'$Path\' is not valid, try to recreate it");
    }
    my $Content = readFile($FilePath);
    rmtree($TMP_DIR."/unpack");
    
    if($Content!~/};\s*\Z/) {
        exitStatus("Invalid_Dump", "specified API dump \'$Path\' is not valid, try to recreate it");
    }
    my $APIDump = eval($Content);
    if(not $APIDump) {
        exitStatus("Error", "internal error - eval() procedure seem to not working correctly, try to remove 'use strict' and try again");
    }
    my $DumpVersion = $APIDump->{"API_DUMP_VERSION"};
    if(majorVersion($DumpVersion) ne $API_DUMP_MAJOR)
    { # compatible with the dumps of the same major version
        exitStatus("Dump_Version", "incompatible version $DumpVersion of specified API dump (allowed only $API_DUMP_MAJOR.0<=V<=$API_DUMP_MAJOR.9)");
    }
    
    if(defined $TypeInfo{$LibVersion})
    {
        foreach my $TId (keys(%{$APIDump->{"TypeInfo"}}))
        {
            $TypeInfo{$LibVersion}{$TId} = $APIDump->{"TypeInfo"}{$TId};
        }
    }
    else {
        $TypeInfo{$LibVersion} = $APIDump->{"TypeInfo"};
    }
    
    foreach my $TypeId (keys(%{$APIDump->{"TypeInfo"}}))
    {
        my %TypeAttr = %{$TypeInfo{$LibVersion}{$TypeId}};
        $TName_Tid{$LibVersion}{$TypeAttr{"Name"}} = $TypeId;
        
        if($Subj ne "Dep")
        {
            if(my $Archive = $TypeAttr{"Archive"}) {
                $LibArchives{$LibVersion}{$Archive} = 1;
            }
        }
        
        foreach my $FieldName (keys(%{$TypeAttr{"Fields"}}))
        {
            if($TypeAttr{"Fields"}{$FieldName}{"Access"}=~/public|protected/) {
                $Class_Fields{$LibVersion}{$TypeAttr{"Name"}}{$FieldName} = $TypeAttr{"Fields"}{$FieldName}{"Type"};
            }
        }
        
        if($Subj eq "Dep") {
            $TypeInfo{$LibVersion}{$TypeId}{"Dep"} = 1;
        }
    }
    my $MInfo = $APIDump->{"MethodInfo"};
    foreach my $M_Id (keys(%{$MInfo}))
    {
        my $Name = $MInfo->{$M_Id}{"Name"};
        $MethodInfo{$LibVersion}{$Name} = $MInfo->{$M_Id};
        
        if($Subj eq "Dep") {
            $MethodInfo{$LibVersion}{$Name}{"Dep"} = 1;
        }
    }
    
    my $MUsed = $APIDump->{"MethodUsed"};
    foreach my $M_Id (keys(%{$MUsed}))
    {
        my $Name = $MUsed->{$M_Id}{"Name"};
        $MethodUsed{$LibVersion}{$Name} = $MUsed->{$M_Id}{"Used"};
    }
    
    foreach my $Method (keys(%{$MethodInfo{$LibVersion}}))
    {
        if(my $ClassId = $MethodInfo{$LibVersion}{$Method}{"Class"}
        and $MethodInfo{$LibVersion}{$Method}{"Access"}=~/public|protected/)
        {
            $Class_Methods{$LibVersion}{get_TypeName($ClassId, $LibVersion)}{$Method} = 1;
            if($MethodInfo{$LibVersion}{$Method}{"Abstract"}) {
                $Class_AbstractMethods{$LibVersion}{get_TypeName($ClassId, $LibVersion)}{$Method} = 1;
            }
        }
    }
    
    # $FieldUsed{$LibVersion} = $APIDump->{"FieldUsed"};
    
    if(keys(%{$LibArchives{$LibVersion}})) {
        $Descriptor{$LibVersion}{"Archives"} = "OK";
    }
    $Descriptor{$LibVersion}{"Version"} = $APIDump->{"LibraryVersion"};
    $Descriptor{$LibVersion}{"Dump"} = 1;
}

sub createDescriptor($$)
{
    my ($LibVersion, $Path) = @_;
    return if(not $LibVersion or not $Path or not -e $Path);
    
    if(isDump($Path))
    { # API dump
        read_API_Dump($LibVersion, $Path, "Main");
    }
    else
    {
        if(-d $Path or $Path=~/\.jar\Z/)
        {
            readDescriptor($LibVersion,"
              <version>
                  ".$TargetVersion{$LibVersion}."
              </version>
              
              <archives>
                  $Path
              </archives>");
        }
        else
        { # standard XML descriptor
            readDescriptor($LibVersion, readFile($Path));
        }
    }
    
    if(my $Dep = $DepDump{$LibVersion}) {
        read_API_Dump($LibVersion, $Dep, "Dep");
    }
}

sub get_version($)
{
    my $Cmd = $_[0];
    return "" if(not $Cmd);
    my $Version = `$Cmd --version 2>\"$TMP_DIR/null\"`;
    return $Version;
}

sub get_depth($)
{
    if(defined $Cache{"get_depth"}{$_[0]}) {
        return $Cache{"get_depth"}{$_[0]}
    }
    return ($Cache{"get_depth"}{$_[0]} = ($_[0]=~tr![\/\\]|\:\:!!));
}

sub show_time_interval($)
{
    my $Interval = $_[0];
    my $Hr = int($Interval/3600);
    my $Min = int($Interval/60)-$Hr*60;
    my $Sec = $Interval-$Hr*3600-$Min*60;
    if($Hr) {
        return "$Hr hr, $Min min, $Sec sec";
    }
    elsif($Min) {
        return "$Min min, $Sec sec";
    }
    else {
        return "$Sec sec";
    }
}

sub checkVersionNum($$)
{
    my ($LibVersion, $Path) = @_;
    
    if($TargetVersion{$LibVersion}) {
        return;
    }
    
    if($Path!~/\.jar\Z/i) {
        return;
    }
    
    my $Ver = undef;
    
    if(not defined $Ver) {
        $Ver = getManifestVersion(get_abs_path($Path));
    }
    
    if(not defined $Ver) {
        $Ver = getPkgVersion(get_filename($Path));
    }
    
    if(not defined $Ver) {
        $Ver = parseVersion($Path);
    }
    
    if(not defined $Ver)
    {
        if($DumpAPI)
        {
            $Ver = "XYZ";
        }
        else
        {
            if($LibVersion==1) {
                $Ver = "X";
            }
            else {
                $Ver = "Y";
            }
        }
    }
    
    $TargetVersion{$LibVersion} = $Ver;
    
    if($DumpAPI) {
        printMsg("WARNING", "set version number to $Ver (use -vnum option to change it)");
    }
    else {
        printMsg("WARNING", "set #$LibVersion version number to $Ver (use --v$LibVersion=NUM option to change it)");
    }
}

sub getManifestVersion($)
{
    my $Path = $_[0];
    
    if(not $Path or not -e $Path) {
        return undef;
    }
    
    my $JarCmd = get_CmdPath("jar");
    if(not $JarCmd) {
        exitStatus("Not_Found", "can't find \"jar\" command");
    }
    chdir($TMP_DIR);
    system($JarCmd." -xf \"$Path\" META-INF 2>null");
    chdir($ORIG_DIR);
    if(my $Content = readFile("$TMP_DIR/META-INF/MANIFEST.MF"))
    {
        if($Content=~/(\A|\s)Implementation\-Version:\s*(.+)(\s|\Z)/i) {
            return $2;
        }
    }
    return undef;
}

sub parseVersion($)
{
    my $Str = $_[0];
    
    if(not $Str) {
        return undef;
    }
    
    if($Str=~/(\/|\\|\w|\A)[\-\_]*(\d+[\d\.\-]+\d+|\d+)/) {
        return $2;
    }
    return undef;
}

sub getPkgVersion($)
{
    my $Name = $_[0];
    $Name=~s/\.\w+\Z//;
    if($Name=~/\A(.+[a-z])[\-\_](v|ver|)(\d.+?)\Z/i)
    { # libsample-N
      # libsample-vN
        return ($1, $3);
    }
    elsif($Name=~/\A(.+?)(\d[\d\.]*)\Z/i)
    { # libsampleN
        return ($1, $2);
    }
    elsif($Name=~/\A(.+)[\-\_](v|ver|)(\d.+?)\Z/i)
    { # libsample-N
      # libsample-vN
        return ($1, $3);
    }
    elsif($Name=~/\A([a-z_\-]+)(\d.+?)\Z/i)
    { # libsampleNb
        return ($1, $2);
    }
    return (undef, undef);
}

sub get_OSgroup()
{
    if($Config{"osname"}=~/macos|darwin|rhapsody/i) {
        return "macos";
    }
    elsif($Config{"osname"}=~/freebsd|openbsd|netbsd/i) {
        return "bsd";
    }
    elsif($Config{"osname"}=~/haiku|beos/i) {
        return "beos";
    }
    elsif($Config{"osname"}=~/symbian|epoc/i) {
        return "symbian";
    }
    elsif($Config{"osname"}=~/win/i) {
        return "windows";
    }
    else {
        return $Config{"osname"};
    }
}

sub get_ARG_MAX()
{
    if($OSgroup eq "windows") {
        return 1990; # 8191, 2047
    }
    else
    { # Linux
      # TODO: set max possible value (~131000)
        return 32767;
    }
}

sub dump_sorting($)
{
    my $Hash = $_[0];
    return [] if(not $Hash);
    my @Keys = keys(%{$Hash});
    return [] if($#Keys<0);
    if($Keys[0]=~/\A\d+\Z/)
    { # numbers
        return [sort {int($a)<=>int($b)} @Keys];
    }
    else
    { # strings
        return [sort {$a cmp $b} @Keys];
    }
}

sub detect_bin_default_paths()
{
    my $EnvPaths = $ENV{"PATH"};
    if($OSgroup eq "beos") {
        $EnvPaths.=":".$ENV{"BETOOLS"};
    }
    elsif($OSgroup eq "windows"
    and my $JHome = $ENV{"JAVA_HOME"}) {
        $EnvPaths.=";$JHome\\bin";
    }
    my $Sep = ($OSgroup eq "windows")?";":":|;";
    foreach my $Path (sort {length($a)<=>length($b)} split(/$Sep/, $EnvPaths))
    {
        $Path=~s/[\/\\]+\Z//g;
        next if(not $Path);
        $DefaultBinPaths{$Path} = 1;
    }
}

sub detect_default_paths()
{
    if(keys(%SystemPaths))
    {# run once
        return;
    }
    
    foreach my $Type (keys(%{$OS_AddPath{$OSgroup}}))
    {# additional search paths
        foreach my $Path (keys(%{$OS_AddPath{$OSgroup}{$Type}}))
        {
            next if(not -d $Path);
            $SystemPaths{$Type}{$Path} = $OS_AddPath{$OSgroup}{$Type}{$Path};
        }
    }
    if($OSgroup ne "windows")
    {
        foreach my $Type ("include", "lib", "bin")
        {# autodetecting system "devel" directories
            foreach my $Path (cmd_find("/","d","*$Type*",1)) {
                $SystemPaths{$Type}{$Path} = 1;
            }
            if(-d "/usr") {
                foreach my $Path (cmd_find("/usr","d","*$Type*",1)) {
                    $SystemPaths{$Type}{$Path} = 1;
                }
            }
        }
    }
    detect_bin_default_paths();
    foreach my $Path (keys(%DefaultBinPaths)) {
        $SystemPaths{"bin"}{$Path} = $DefaultBinPaths{$Path};
    }
    
    if(not $TestSystem)
    {
        if(my $JavacCmd = get_CmdPath("javac"))
        {
            if(my $Ver = `$JavacCmd -version 2>&1`)
            {
                if($Ver=~/javac\s+(.+)/) {
                    printMsg("INFO", "using Java ".$1);
                }
            }
        }
    }
}

sub exitStatus($$)
{
    my ($Code, $Msg) = @_;
    print STDERR "ERROR: ". $Msg."\n";
    exit($ERROR_CODE{$Code});
}

sub printMsg($$)
{
    my ($Type, $Msg) = @_;
    if($Type!~/\AINFO/) {
        $Msg = $Type.": ".$Msg;
    }
    if($Type!~/_C\Z/) {
        $Msg .= "\n";
    }
    if($Type eq "ERROR") {
        print STDERR $Msg;
    }
    else {
        print $Msg;
    }
}

sub printStatMsg($)
{
    my $Level = $_[0];
    printMsg("INFO", "total ".lc($Level)." compatibility problems: ".$RESULT{$Level}{"Problems"}.", warnings: ".$RESULT{$Level}{"Warnings"});
}

sub printReport()
{
    printMsg("INFO", "creating compatibility report ...");
    createReport();
    if($JoinReport or $DoubleReport)
    {
        if($RESULT{"Binary"}{"Problems"}
        or $RESULT{"Source"}{"Problems"})
        {
            printMsg("INFO", "binary compatibility: ".(100-$RESULT{"Binary"}{"Affected"})."\%");
            printMsg("INFO", "source compatibility: ".(100-$RESULT{"Source"}{"Affected"})."\%");
        }
        else
        {
            printMsg("INFO", "binary compatibility: 100\%");
            printMsg("INFO", "source compatibility: 100\%");
        }
        printStatMsg("Binary");
        printStatMsg("Source");
    }
    elsif($BinaryOnly)
    {
        if($RESULT{"Binary"}{"Problems"}) {
            printMsg("INFO", "binary compatibility: ".(100-$RESULT{"Binary"}{"Affected"})."\%");
        }
        else {
            printMsg("INFO", "binary compatibility: 100\%");
        }
        printStatMsg("Binary");
    }
    elsif($SourceOnly)
    {
        if($RESULT{"Source"}{"Problems"}) {
            printMsg("INFO", "source compatibility: ".(100-$RESULT{"Source"}{"Affected"})."\%");
        }
        else {
            printMsg("INFO", "source compatibility: 100\%");
        }
        printStatMsg("Source");
    }
    if($JoinReport)
    {
        printMsg("INFO", "report: ".getReportPath("Join"));
    }
    elsif($DoubleReport)
    { # default
        printMsg("INFO", "report (BC): ".getReportPath("Binary"));
        printMsg("INFO", "report (SC): ".getReportPath("Source"));
    }
    elsif($BinaryOnly)
    { # --binary
        printMsg("INFO", "report: ".getReportPath("Binary"));
    }
    elsif($SourceOnly)
    { # --source
        printMsg("INFO", "report: ".getReportPath("Source"));
    }
}

sub getReportPath($)
{
    my $Level = $_[0];
    my $Dir = "compat_reports/$TargetLibraryName/".$Descriptor{1}{"Version"}."_to_".$Descriptor{2}{"Version"};
    if($Level eq "Binary")
    {
        if($BinaryReportPath)
        { # --bin-report-path
            return $BinaryReportPath;
        }
        elsif($OutputReportPath)
        { # --report-path
            return $OutputReportPath;
        }
        else
        { # default
            return $Dir."/bin_compat_report.html";
        }
    }
    elsif($Level eq "Source")
    {
        if($SourceReportPath)
        { # --src-report-path
            return $SourceReportPath;
        }
        elsif($OutputReportPath)
        { # --report-path
            return $OutputReportPath;
        }
        else
        { # default
            return $Dir."/src_compat_report.html";
        }
    }
    else
    {
        if($OutputReportPath)
        { # --report-path
            return $OutputReportPath;
        }
        else
        { # default
            return $Dir."/compat_report.html";
        }
    }
}

sub initLogging($)
{
    my $LibVersion = $_[0];
    if($Debug)
    { # debug directory
        $DEBUG_PATH{$LibVersion} = "debug/$TargetLibraryName/".$Descriptor{$LibVersion}{"Version"};
        
        if(-d $DEBUG_PATH{$LibVersion}) {
            rmtree($DEBUG_PATH{$LibVersion});
        }
    }
}

sub createArchive($$)
{
    my ($Path, $To) = @_;
    if(not $To) {
        $To = ".";
    }
    if(not $Path or not -e $Path
    or not -d $To) {
        return "";
    }
    my ($From, $Name) = separate_path($Path);
    if($OSgroup eq "windows")
    { # *.zip
        my $ZipCmd = get_CmdPath("zip");
        if(not $ZipCmd) {
            exitStatus("Not_Found", "can't find \"zip\"");
        }
        my $Pkg = $To."/".$Name.".zip";
        unlink($Pkg);
        chdir($To);
        system("$ZipCmd -j \"$Name.zip\" \"$Path\" >\"$TMP_DIR/null\"");
        if($?)
        { # cannot allocate memory (or other problems with "zip")
            unlink($Path);
            exitStatus("Error", "can't pack the API dump: ".$!);
        }
        chdir($ORIG_DIR);
        unlink($Path);
        return $Pkg;
    }
    else
    { # *.tar.gz
        my $TarCmd = get_CmdPath("tar");
        if(not $TarCmd) {
            exitStatus("Not_Found", "can't find \"tar\"");
        }
        my $GzipCmd = get_CmdPath("gzip");
        if(not $GzipCmd) {
            exitStatus("Not_Found", "can't find \"gzip\"");
        }
        my $Pkg = abs_path($To)."/".$Name.".tar.gz";
        unlink($Pkg);
        chdir($From);
        system($TarCmd, "-czf", $Pkg, $Name);
        if($?)
        { # cannot allocate memory (or other problems with "tar")
            unlink($Path);
            exitStatus("Error", "can't pack the API dump: ".$!);
        }
        chdir($ORIG_DIR);
        unlink($Path);
        return $To."/".$Name.".tar.gz";
    }
}

sub getMd5(@)
{
    my $Md5 = md5_hex(@_);
    return substr($Md5, 0, $MD5_LEN);
}

sub scenario()
{
    if($BinaryOnly and $SourceOnly)
    { # both --binary and --source
      # is the default mode
        $DoubleReport = 1;
        $JoinReport = 0;
        $BinaryOnly = 0;
        $SourceOnly = 0;
        if($OutputReportPath)
        { # --report-path
            $DoubleReport = 0;
            $JoinReport = 1;
        }
    }
    elsif($BinaryOnly or $SourceOnly)
    { # --binary or --source
        $DoubleReport = 0;
        $JoinReport = 0;
    }
    if(defined $Help)
    {
        HELP_MESSAGE();
        exit(0);
    }
    if(defined $ShowVersion)
    {
        printMsg("INFO", "Java API Compliance Checker (JAPICC) $TOOL_VERSION\nCopyright (C) 2016 Andrey Ponomarenko's ABI Laboratory\nLicense: LGPL or GPL <http://www.gnu.org/licenses/>\nThis program is free software: you can redistribute it and/or modify it.\n\nWritten by Andrey Ponomarenko.");
        exit(0);
    }
    if(defined $DumpVersion)
    {
        printMsg("INFO", $TOOL_VERSION);
        exit(0);
    }
    $Data::Dumper::Sortkeys = 1;
    
    # FIXME: can't pass \&dump_sorting - cause a segfault sometimes
    if($SortDump)
    {
        $Data::Dumper::Useperl = 1;
        $Data::Dumper::Sortkeys = \&dump_sorting;
    }
    
    if(defined $TestSystem)
    {
        detect_default_paths();
        testSystem();
        exit(0);
    }
    
    if(defined $ShortMode)
    {
        if(not defined $AffectLimit) {
            $AffectLimit = 10;
        }
    }
    
    if(not $TargetLibraryName and not $CountMethods)
    {
        if($DumpAPI)
        {
            if($DumpAPI=~/\.jar\Z/)
            { # short usage
                my ($Name, $Version) = getPkgVersion(get_filename($DumpAPI));
                if($Name and $Version ne "")
                {
                    $TargetLibraryName = $Name;
                    if(not $TargetVersion{1}) {
                        $TargetVersion{1} = $Version;
                    }
                }
            }
        }
        else
        {
            if($Descriptor{1}{"Path"}=~/\.jar\Z/ and $Descriptor{2}{"Path"}=~/\.jar\Z/)
            { # short usage
                my ($Name1, $Version1) = getPkgVersion(get_filename($Descriptor{1}{"Path"}));
                my ($Name2, $Version2) = getPkgVersion(get_filename($Descriptor{2}{"Path"}));
                
                if($Name1 and $Version1 ne ""
                and $Version2 ne "")
                {
                    $TargetLibraryName = $Name1;
                    if(not $TargetVersion{1}) {
                        $TargetVersion{1} = $Version1;
                    }
                    if(not $TargetVersion{2}) {
                        $TargetVersion{2} = $Version2;
                    }
                }
            }
        }
        
        if(not $TargetLibraryName) {
            exitStatus("Error", "library name is not selected (option --lib=NAME)");
        }
    }
    else
    { # validate library name
        if($TargetLibraryName=~/[\*\/\\]/) {
            exitStatus("Error", "\"\\\", \"\/\" and \"*\" symbols are not allowed in the library name");
        }
    }
    if(not $TargetTitle) {
        $TargetTitle = $TargetLibraryName;
    }
    if($ClassListPath)
    {
        if(not -f $ClassListPath) {
            exitStatus("Access_Error", "can't access file \'$ClassListPath\'");
        }
        foreach my $Class (split(/\n/, readFile($ClassListPath)))
        {
            $Class=~s/\//./g;
            $ClassList_User{$Class} = 1;
        }
    }
    if($AnnotationsListPath)
    {
        if(not -f $AnnotationsListPath) {
            exitStatus("Access_Error", "can't access file \'$AnnotationsListPath\'");
        }
        foreach my $Annotation (split(/\n/, readFile($AnnotationsListPath)))
        {
            $AnnotationList_User{$Annotation} = 1;
        }
    }
    if($SkipAnnotationsListPath)
    {
        if(not -f $SkipAnnotationsListPath) {
            exitStatus("Access_Error", "can't access file \'$SkipAnnotationsListPath\'");
        }
        foreach my $Annotation (split(/\n/, readFile($SkipAnnotationsListPath)))
        {
            $SkipAnnotationList_User{$Annotation} = 1;
        }
    }
    if($SkipClassesList)
    {
        if(not -f $SkipClassesList) {
            exitStatus("Access_Error", "can't access file \'$SkipClassesList\'");
        }
        foreach my $Class (split(/\n/, readFile($SkipClassesList)))
        {
            $Class=~s/\//./g;
            $SkipClasses{$Class} = 1;
        }
    }
    if($SkipPackagesList)
    {
        if(not -f $SkipPackagesList) {
            exitStatus("Access_Error", "can't access file \'$SkipPackagesList\'");
        }
        foreach my $Package (split(/\n/, readFile($SkipPackagesList)))
        {
            $SkipPackages{1}{$Package} = 1;
            $SkipPackages{2}{$Package} = 1;
        }
    }
    if($ClientPath)
    {
        if($ClientPath=~/\.class\Z/) {
            exitStatus("Error", "input file is not a java archive");
        }
        
        if(-f $ClientPath)
        {
            detect_default_paths();
            readArchive(0, $ClientPath)
        }
        else {
            exitStatus("Access_Error", "can't access file \'$ClientPath\'");
        }
    }
    
    if($CountMethods)
    {
        if(not -e $CountMethods) {
            exitStatus("Access_Error", "can't access \'$CountMethods\'");
        }
        
        read_API_Dump(1, $CountMethods, "Main");
        
        my $Count = 0;
        foreach my $Method (keys(%{$MethodInfo{1}})) {
            $Count += methodFilter($Method, 1);
        }
        
        printMsg("INFO", $Count);
        exit(0);
    }
    
    if($DumpAPI)
    {
        if(not -e $DumpAPI) {
            exitStatus("Access_Error", "can't access \'$DumpAPI\'");
        }
        
        detect_default_paths();
        checkVersionNum(1, $DumpAPI);
        
        my $TarCmd = get_CmdPath("tar");
        if(not $TarCmd) {
            exitStatus("Not_Found", "can't find \"tar\"");
        }
        my $GzipCmd = get_CmdPath("gzip");
        if(not $GzipCmd) {
            exitStatus("Not_Found", "can't find \"gzip\"");
        }
        createDescriptor(1, $DumpAPI);
        if(not $Descriptor{1}{"Archives"}) {
            exitStatus("Error", "descriptor does not contain Java ARchives");
        }
        
        initLogging(1);
        readArchives(1);
        
        printMsg("INFO", "creating library API dump ...");
        if(not keys(%{$MethodInfo{1}})) {
            printMsg("WARNING", "empty dump");
        }
        
        my $MInfo = {};
        my $MNum = 0;
        foreach my $Method (sort keys(%{$MethodInfo{1}}))
        {
            my $MId = $MNum;
            if($REPRODUCIBLE) {
                $MId = getMd5($Method);
            }
            $MInfo->{$MId} = $MethodInfo{1}{$Method};
            $MInfo->{$MId}{"Name"} = $Method;
            
            $MNum+=1;
        }
        
        my $MUsed = {};
        $MNum = 0;
        foreach my $Inv (sort keys(%{$MethodUsed{1}}))
        {
            my $MId = $MNum;
            if($REPRODUCIBLE) {
                $MId = getMd5($Inv);
            }
            
            $MUsed->{$MId}{"Name"} = $Inv;
            $MUsed->{$MId}{"Used"} = $MethodUsed{1}{$Inv};
            
            $MNum+=1;
        }
        
        my %API = (
            "MethodInfo" => $MInfo,
            "TypeInfo" => $TypeInfo{1},
            "MethodUsed" => $MUsed,
            # "FieldUsed" => $FieldUsed{1},
            "LibraryVersion" => $Descriptor{1}{"Version"},
            "LibraryName" => $TargetLibraryName,
            "Language" => "Java",
            "API_DUMP_VERSION" => $API_DUMP_VERSION,
            "JAPI_COMPLIANCE_CHECKER_VERSION" => $TOOL_VERSION
        );
        
        my $DumpPath = "api_dumps/$TargetLibraryName/".$TargetLibraryName."_".$Descriptor{1}{"Version"}.".api.".$AR_EXT;
        if($OutputDumpPath)
        { # user defined path
            $DumpPath = $OutputDumpPath;
        }
        
        my $Archive = ($DumpPath=~s/\Q.$AR_EXT\E\Z//g);
        
        my ($DDir, $DName) = separate_path($DumpPath);
        my $DPath = $TMP_DIR."/".$DName;
        if(not $Archive) {
            $DPath = $DumpPath;
        }
        
        mkpath($DDir);
        
        open(DUMP, ">", $DPath) || die ("can't open file \'$DPath\': $!\n");
        print DUMP Dumper(\%API);
        close(DUMP);
        
        if(not -s $DPath) {
            exitStatus("Error", "can't create API dump because something is going wrong with the Data::Dumper module");
        }
        
        if($Archive) {
            $DumpPath = createArchive($DPath, $DDir);
        }
        
        if($OutputDumpPath) {
            printMsg("INFO", "dump path: $OutputDumpPath");
        }
        else {
            printMsg("INFO", "dump path: $DumpPath");
        }
        exit(0);
    }
    if(not $Descriptor{1}{"Path"}) {
        exitStatus("Error", "-old option is not specified");
    }
    if(not -e $Descriptor{1}{"Path"}) {
        exitStatus("Access_Error", "can't access \'".$Descriptor{1}{"Path"}."\'");
    }
    if(not $Descriptor{2}{"Path"}) {
        exitStatus("Error", "-new option is not specified");
    }
    if(not -e $Descriptor{2}{"Path"}) {
        exitStatus("Access_Error", "can't access \'".$Descriptor{2}{"Path"}."\'");
    }
    
    if($Quick)
    {
        $TypeProblems_Kind{"Binary"}{"Interface_Added_Super_Interface"} = "Low";
        $TypeProblems_Kind{"Binary"}{"Abstract_Class_Added_Super_Abstract_Class"} = "Low";
        $TypeProblems_Kind{"Binary"}{"Abstract_Class_Added_Super_Interface"} = "Low";
        $TypeProblems_Kind{"Binary"}{"Abstract_Class_Added_Abstract_Method"} = "Low";
        $TypeProblems_Kind{"Binary"}{"Interface_Added_Abstract_Method"} = "Low";
    }
    
    detect_default_paths();
    
    checkVersionNum(1, $Descriptor{1}{"Path"});
    checkVersionNum(2, $Descriptor{2}{"Path"});
    
    createDescriptor(1, $Descriptor{1}{"Path"});
    createDescriptor(2, $Descriptor{2}{"Path"});
    
    if(not $Descriptor{1}{"Archives"}) {
        exitStatus("Error", "descriptor d1 does not contain Java ARchives");
    }
    if(not $Descriptor{2}{"Archives"}) {
        exitStatus("Error", "descriptor d2 does not contain Java ARchives");
    }
    initLogging(1);
    initLogging(2);
    
    if($Descriptor{1}{"Archives"}
    and not $Descriptor{1}{"Dump"}) {
        readArchives(1);
    }
    if($Descriptor{2}{"Archives"}
    and not $Descriptor{2}{"Dump"}) {
        readArchives(2);
    }
    
    foreach my $Inv (keys(%{$MethodUsed{2}}))
    {
        foreach my $M (keys(%{$MethodUsed{2}{$Inv}}))
        {
            my $InvType = $MethodUsed{2}{$Inv}{$M};
            
            if($InvType ne "static"
            and index($Inv, "<init>")==-1)
            {
                my $CName = $Inv;
                $CName=~s/\A\"\[L(.+);"/$1/g;
                $CName=~s/#/./g;
                
                if($CName=~/\A(.+?)\./)
                {
                    $CName = $1;
                    if($CName!~/\"/)
                    {
                        $CName=~s!/!.!g;
                        $ClassMethod_AddedUsed{$CName}{$Inv} = $M;
                    }
                }
            }
            
            if(not defined $MethodInfo{1}{$M}) {
                delete($MethodUsed{2}{$Inv}{$M});
            }
        }
    }
    
    foreach my $ClassName (keys(%ClassMethod_AddedUsed))
    {
        foreach my $MethodName (keys(%{$ClassMethod_AddedUsed{$ClassName}}))
        {
            if(defined $MethodInfo{1}{$MethodName}
            or defined $MethodInfo{2}{$MethodName}
            or defined $MethodUsed{1}{$MethodName}
            or findMethod($MethodName, 2, $ClassName, 1))
            { # abstract method added by the new super-class (abstract) or super-interface
                delete($ClassMethod_AddedUsed{$ClassName}{$MethodName});
            }
        }
        if(not keys(%{$ClassMethod_AddedUsed{$ClassName}})) {
            delete($ClassMethod_AddedUsed{$ClassName});
        }
    }
    
    prepareMethods(1);
    prepareMethods(2);
    
    detectAdded();
    detectRemoved();
    
    printMsg("INFO", "comparing classes ...");
    mergeClasses();
    mergeMethods();
    
    foreach my $M (keys(%CompatProblems))
    {
        foreach my $K (keys(%{$CompatProblems{$M}}))
        {
            foreach my $L (keys(%{$CompatProblems{$M}{$K}}))
            {
                if(my $T = $CompatProblems{$M}{$K}{$L}{"Type_Name"}) {
                    $TypeProblemsIndex{$T}{$M} = 1;
                }
            }
        }
    }
    
    printReport();
    
    if($RESULT{"Source"}{"Problems"} + $RESULT{"Binary"}{"Problems"}) {
        exit($ERROR_CODE{"Incompatible"});
    }
    else {
        exit($ERROR_CODE{"Compatible"});
    }
}

scenario();

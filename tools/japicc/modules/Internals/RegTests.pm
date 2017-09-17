###########################################################################
# A module with regression test suite
#
# Copyright (C) 2016-2017 Andrey Ponomarenko's ABI Laboratory
#
# Written by Andrey Ponomarenko
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
use strict;
use File::Copy qw(copy);

sub testTool()
{
    printMsg("INFO", "\nVerifying detectable Java library changes");
    
    printMsg("INFO", "Creating test library ...");
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
    
    if(cmpVersions($In::Opt{"CompilerVer"}, "1.5")>=0)
    {
        # GenericClass1
        writeFile($Path_v1."/GenericClass1.java",
        "package $PackageName;
        public class GenericClass1<T> {
            public Integer method(T param) { return 0; }
        }");
        writeFile($Path_v2."/GenericClass1.java",
        "package $PackageName;
        public class GenericClass1<T> {
            public Integer method(T param) { return 0; }
        }");
        
        # GenericClass2
        writeFile($Path_v1."/GenericClass2.java",
        "package $PackageName;
        public class GenericClass2<T> {
            public void method() { }
        }");
        writeFile($Path_v2."/GenericClass2.java",
        "package $PackageName;
        public class GenericClass2<T> {
            public void method() { }
        }");
        
        # Class became generic
        writeFile($Path_v1."/ClassBecameGeneric.java",
        "package $PackageName;
        public abstract class ClassBecameGeneric {
            public ClassBecameGeneric() {}
            public abstract ClassBecameGeneric doSomething();
        }");
        writeFile($Path_v2."/ClassBecameGeneric.java",
        "package $PackageName;
        public abstract class ClassBecameGeneric<T> {
            public ClassBecameGeneric() {}
            public abstract T doSomething();
        }");
        
        writeFile($TestsPath."/Test_ClassBecameGeneric.java",
        "import $PackageName.*;
        class GenerifyingClassDerived extends ClassBecameGeneric {
            public ClassBecameGeneric doSomething() { return new GenerifyingClassDerived(); }
            public static void main(String[] args) { }
        }
        public class Test_ClassBecameGeneric
        {
            public static void main(String[] args) {
                GenerifyingClassDerived X = new GenerifyingClassDerived();
                ClassBecameGeneric Res = X.doSomething();
            }
        }");
        
        # Class became raw
        writeFile($Path_v1."/ClassBecameRaw.java",
        "package $PackageName;
        public class ClassBecameRaw<T extends String> {
            public void method(T param) { }
        }");
        writeFile($Path_v2."/ClassBecameRaw.java",
        "package $PackageName;
        public class ClassBecameRaw {
            public void method(String param) { }
        }");
        
        writeFile($TestsPath."/Test_ClassBecameRaw.java",
        "import $PackageName.*;
        public class Test_ClassBecameRaw
        {
            public static void main(String[] args) {
                ClassBecameRaw<String> X = new ClassBecameRaw<String>();
                X.method(\"XXX\");
            }
        }");
        
        # Interface became generic
        writeFile($Path_v1."/InterfaceBecameGeneric.java",
        "package $PackageName;
        public interface InterfaceBecameGeneric {
            public void method();
        }");
        writeFile($Path_v2."/InterfaceBecameGeneric.java",
        "package $PackageName;
        public interface InterfaceBecameGeneric<T> {
            public void method();
        }");
        
        # Interface became raw
        writeFile($Path_v1."/InterfaceBecameRaw.java",
        "package $PackageName;
        public interface InterfaceBecameRaw<T> {
            public void method();
        }");
        writeFile($Path_v2."/InterfaceBecameRaw.java",
        "package $PackageName;
        public interface InterfaceBecameRaw {
            public void method();
        }");
        
        writeFile($TestsPath."/Test_InterfaceBecameRaw.java",
        "import $PackageName.*;
        class InterfaceBecameRawDerived implements InterfaceBecameRaw<String> {
            public void method() { }
            public static void main(String[] args) { }
        }
        public class Test_InterfaceBecameRaw
        {
            public static void main(String[] args) {
                InterfaceBecameRawDerived X = new InterfaceBecameRawDerived();
                X.method();
            }
        }");
        
        # Changed generic super-class
        writeFile($Path_v1."/GenericSuperClassChanged.java",
        "package $PackageName;
        public class GenericSuperClassChanged extends GenericClass1<String> {
            public Integer method() { return 0; }
        }");
        writeFile($Path_v2."/GenericSuperClassChanged.java",
        "package $PackageName;
        public class GenericSuperClassChanged extends GenericClass1<Integer> {
            public Integer method() { return 0; }
        }");
        
        # Extending class with generic parameters
        writeFile($Path_v1."/ExtendingClassWithGeneric.java",
        "package $PackageName;
        public class ExtendingClassWithGeneric {
            public void method() { }
        }");
        writeFile($Path_v2."/ExtendingClassWithGeneric.java",
        "package $PackageName;
        public class ExtendingClassWithGeneric extends GenericClass2<String>
        {
        }");
        
        # Renamed generic parameter
        writeFile($Path_v1."/RenamedGenericParameter.java",
        "package $PackageName;
        public class RenamedGenericParameter<A extends String> {
            public void method(A param) { }
        }");
        writeFile($Path_v2."/RenamedGenericParameter.java",
        "package $PackageName;
        public class RenamedGenericParameter<B extends String> {
            public void method(B param) { }
        }");
        
        # Changed field type by introducing of a generic parameter
        writeFile($Path_v1."/ChangedFieldTypeByGenericParam.java",
        "package $PackageName;
        public class ChangedFieldTypeByGenericParam {
            public ChangedFieldTypeByGenericParam(String param) { f=param; }
            public void method() { }
            public String f;
        }");
        writeFile($Path_v2."/ChangedFieldTypeByGenericParam.java",
        "package $PackageName;
        public class ChangedFieldTypeByGenericParam<T> {
            public ChangedFieldTypeByGenericParam(T param) { f=param; }
            public void method() { }
            public T f;
        }");
        
        writeFile($Path_v1."/TestGeneric.java",
        "package $PackageName;
        public class TestGeneric {
            public ChangedFieldTypeByGenericParam get1() { return new ChangedFieldTypeByGenericParam(\"XXX\"); }
            public ChangedFieldTypeByGenericParam get2() { return new ChangedFieldTypeByGenericParam(\"XXX\"); }
        }");
        writeFile($Path_v2."/TestGeneric.java",
        "package $PackageName;
        public class TestGeneric {
            public ChangedFieldTypeByGenericParam<String>  get1() { return new ChangedFieldTypeByGenericParam<String>(\"XXX\"); }
            public ChangedFieldTypeByGenericParam<Integer> get2() { return new ChangedFieldTypeByGenericParam<Integer>(0); }
        }");
        
        writeFile($TestsPath."/Test_ChangedFieldTypeByGenericParam.java",
        "import $PackageName.*;
        public class Test_ChangedFieldTypeByGenericParam
        {
            public static void main(String[] args)
            {
                TestGeneric X = new TestGeneric();
                ChangedFieldTypeByGenericParam Res1 = X.get1();
                ChangedFieldTypeByGenericParam Res2 = X.get2();
                Res1.f = Res2.f;
            }
        }");
        
        # Changed constructor after generifying
        writeFile($Path_v1."/ChangedCtorAfterGenerifying.java",
        "package $PackageName;
        public class ChangedCtorAfterGenerifying {
            public ChangedCtorAfterGenerifying(String param) { }
            public String f;
        }");
        writeFile($Path_v2."/ChangedCtorAfterGenerifying.java",
        "package $PackageName;
        public class ChangedCtorAfterGenerifying<T> {
            public ChangedCtorAfterGenerifying(T param) { }
            public T f;
        }");
        
        writeFile($TestsPath."/Test_ChangedCtorAfterGenerifying.java",
        "import $PackageName.*;
        public class Test_ChangedCtorAfterGenerifying
        {
            public static void main(String[] args) {
                ChangedCtorAfterGenerifying X = new ChangedCtorAfterGenerifying(\"XXX\");
            }
        }");
        
        # Array to variable arity
        writeFile($Path_v1."/ArrayToVariableArity.java",
        "package $PackageName;
        public class ArrayToVariableArity {
            public void method(Integer x, String[] y) { }
        }");
        writeFile($Path_v2."/ArrayToVariableArity.java",
        "package $PackageName;
        public class ArrayToVariableArity {
            public void method(Integer x, String... y) { }
        }");
        
        writeFile($TestsPath."/Test_ArrayToVariableArity.java",
        "import $PackageName.*;
        public class Test_ArrayToVariableArity
        {
            public static void main(String[] args) {
                ArrayToVariableArity X = new ArrayToVariableArity();
                X.method(0, new String[]{\"a\", \"b\"});
            }
        }");
        
        # Variable arity to array
        writeFile($Path_v1."/VariableArityToArray.java",
        "package $PackageName;
        public class VariableArityToArray {
            public void method(Integer x, String... y) { }
        }");
        writeFile($Path_v2."/VariableArityToArray.java",
        "package $PackageName;
        public class VariableArityToArray {
            public void method(Integer x, String[] y) { }
        }");
        
        writeFile($TestsPath."/Test_VariableArityToArray.java",
        "import $PackageName.*;
        public class Test_VariableArityToArray
        {
            public static void main(String[] args) {
                VariableArityToArray X = new VariableArityToArray();
                X.method(0, \"a\", \"b\");
            }
        }");
        
        # Removed_Annotation
        writeFile($Path_v1."/RemovedAnnotation.java",
        "package $PackageName;
        public \@interface RemovedAnnotation {
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
        
        # Beta Annotation
        writeFile($Path_v1."/Beta.java",
        "package $PackageName;
        public \@interface Beta {
        }");
        
        writeFile($Path_v2."/Beta.java",
        "package $PackageName;
        public \@interface Beta {
        }");
        
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
    }
    
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
        public void changedMethod(Integer param1) { }
    }");
    writeFile($Path_v2."/ChangedMethodReturnFromVoid.java",
    "package $PackageName;
    public class ChangedMethodReturnFromVoid {
        public Integer changedMethod(Integer param1){
            return param1;
        }
    }");
    
    writeFile($TestsPath."/Test_ChangedMethodReturnFromVoid.java",
    "import $PackageName.*;
    public class Test_ChangedMethodReturnFromVoid {
        public static void main(String[] args) {
            ChangedMethodReturnFromVoid X = new ChangedMethodReturnFromVoid();
            X.changedMethod(1);
        }
    }");
    
    # Changed_Method_Return
    writeFile($Path_v1."/ChangedMethodReturn.java",
    "package $PackageName;
    public class ChangedMethodReturn {
        public Integer changedMethod(Integer param) { return 0; }
    }");
    writeFile($Path_v2."/ChangedMethodReturn.java",
    "package $PackageName;
    public class ChangedMethodReturn {
        public String changedMethod(Integer param) { return \"XXX\"; }
    }");
    
    writeFile($TestsPath."/Test_ChangedMethodReturn.java",
    "import $PackageName.*;
    public class Test_ChangedMethodReturn {
        public static void main(String[] args) {
            ChangedMethodReturn X = new ChangedMethodReturn();
            Integer Res = X.changedMethod(0);
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
    
    writeFile($TestsPath."/Test_ChangedFieldType.java",
    "import $PackageName.*;
    public class Test_ChangedFieldType {
        public static void main(String[] args) {
            ChangedFieldType X = new ChangedFieldType();
            String R = X.fieldName;
        }
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
    
    writeFile($TestsPath."/Test_NonConstantFieldBecameStatic.java",
    "import $PackageName.*;
    public class Test_NonConstantFieldBecameStatic {
        public static void main(String[] args) {
            NonConstantFieldBecameStatic X = new NonConstantFieldBecameStatic();
            String R = X.fieldName;
        }
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
    
    # Removed protected method from final class
    writeFile($Path_v1."/RemovedProtectedMethodFromFinalClass.java",
    "package $PackageName;
    public final class RemovedProtectedMethodFromFinalClass {
        protected void removedMethod(Integer param) { }
    }");
    writeFile($Path_v2."/RemovedProtectedMethodFromFinalClass.java",
    "package $PackageName;
    public final class RemovedProtectedMethodFromFinalClass {
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
    
    # Interface_Removed_Abstract_Method (Last)
    writeFile($Path_v1."/InterfaceRemovedLastAbstractMethod.java",
    "package $PackageName;
    public interface InterfaceRemovedLastAbstractMethod {
        public void removedMethod(Integer param);
    }");
    writeFile($Path_v2."/InterfaceRemovedLastAbstractMethod.java",
    "package $PackageName;
    public interface InterfaceRemovedLastAbstractMethod {
    }");
    
    writeFile($TestsPath."/Test_InterfaceRemovedLastAbstractMethod.java",
    "import $PackageName.*;
    class InterfaceRemovedLastAbstractMethodDerived implements InterfaceRemovedLastAbstractMethod
    {
        public void removedMethod(Integer param) { }
        public static void main(String[] args) { }
    };
    
    public class Test_InterfaceRemovedLastAbstractMethod
    {
        public static void main(String[] args)
        {
            InterfaceRemovedLastAbstractMethod Obj = new InterfaceRemovedLastAbstractMethodDerived();
            Obj.removedMethod(0);
        }
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
        public static void main(String[] args) { }
    };
    
    public class Test_MethodBecameNonDefault
    {
        public static void main(String[] args)
        {
            Class_MethodBecameNonDefault Obj = new Class_MethodBecameNonDefault();
            Integer Res = Obj.someMethod(0);
        }
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
    public class Test_RemovedFieldClass
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
        public abstract void removedMethod(Integer param);
    }");
    writeFile($Path_v2."/ClassRemovedAbstractMethod.java",
    "package $PackageName;
    public abstract class ClassRemovedAbstractMethod {
        public Integer someMethod(Integer param1, String[] param2) {
            return param1;
        };
    }");
    
    writeFile($TestsPath."/Test_ClassRemovedAbstractMethod.java",
    "import $PackageName.*;
    class ClassRemovedAbstractMethodDerived extends ClassRemovedAbstractMethod
    {
        public void removedMethod(Integer param) { }
        public static void main(String[] args) { }
    };
    
    public class Test_ClassRemovedAbstractMethod
    {
        public static void main(String[] args)
        {
            ClassRemovedAbstractMethod Obj = new ClassRemovedAbstractMethodDerived();
            Obj.removedMethod(0);
        }
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
    writeFile($Path_v2."/StaticMethodBecameFinal.java",
    "package $PackageName;
    public class StaticMethodBecameFinal {
        public static Integer someMethod(Integer param) {
            return param;
        };
    }");
    writeFile($Path_v1."/StaticMethodBecameFinal.java",
    "package $PackageName;
    public class StaticMethodBecameFinal {
        public static final Integer someMethod(Integer param) {
            return param;
        };
    }");
    
    writeFile($TestsPath."/Test_StaticMethodBecameFinal.java",
    "import $PackageName.*;
    public class Test_StaticMethodBecameFinal
    {
        public static void main(String[] args)
        {
            Integer R = StaticMethodBecameFinal.someMethod(0);
        }
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
        public Integer someMethod(InterfaceRemovedLastAbstractMethod param) {
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
        public Integer someMethod(InterfaceRemovedLastAbstractMethod param) {
            return 0;
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
    my $BuildRoot1 = getDirname($Path_v1);
    my $BuildRoot2 = getDirname($Path_v2);
    if(compileJavaLib($LibName, $BuildRoot1, $BuildRoot2))
    {
        runTests($TestsPath, $PackageName, getAbsPath($BuildRoot1), getAbsPath($BuildRoot2));
        runChecker($LibName, $BuildRoot1, $BuildRoot2);
    }
}

sub checkJavaCompiler($)
{ # check javac: compile simple program
    my $Cmd = $_[0];
    
    if(not $Cmd) {
        return;
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    writeFile($TmpDir."/test_javac/Simple.java",
    "public class Simple {
        public Integer f;
        public void method(Integer p) { };
    }");
    chdir($TmpDir."/test_javac");
    system("$Cmd Simple.java 2>errors.txt");
    chdir($In::Opt{"OrigDir"});
    if($?)
    {
        my $Msg = "something is going wrong with the Java compiler (javac):\n";
        my $Err = readFile($TmpDir."/test_javac/errors.txt");
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
    
    printMsg("INFO", "Running tests ...");
    
    # compile with old version of package
    my $JavacCmd = getCmdPath("javac");
    if(not $JavacCmd) {
        exitStatus("Not_Found", "can't find \"javac\" compiler");
    }
    
    my $JavaCmd = getCmdPath("java");
    if(not $JavaCmd) {
        exitStatus("Not_Found", "can't find \"java\" command");
    }
    
    chdir($TestsPath);
    system($JavacCmd." -classpath \"".$Path_v1."\" -g *.java");
    chdir($In::Opt{"OrigDir"});
    
    foreach my $TestSrc (cmdFind($TestsPath, "", "*\\.java"))
    { # remove test source
        unlink($TestSrc);
    }
    
    my $TEST_REPORT = "";
    
    foreach my $TestPath (cmdFind($TestsPath, "", "*\\.class", 1))
    { # run tests
        my $Name = getFilename($TestPath);
        $Name=~s/\.class\Z//g;
        
        chdir($TestsPath);
        system($JavaCmd." -classpath \"".join_A($Path_v2, ".")."\" $Name >result.txt 2>&1");
        chdir($In::Opt{"OrigDir"});
        
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
    
    my $Journal = $TestsPath."/Journal.txt";
    writeFile($Journal, $TEST_REPORT);
    printMsg("INFO", "See journal with test results: $Journal");
}

sub compileJavaLib($$$)
{
    my ($LibName, $BuildRoot1, $BuildRoot2) = @_;
    
    my $JavacCmd = getCmdPath("javac");
    if(not $JavacCmd) {
        exitStatus("Not_Found", "can't find \"javac\" compiler");
    }
    
    checkJavaCompiler($JavacCmd);
    
    my $JarCmd = getCmdPath("jar");
    if(not $JarCmd) {
        exitStatus("Not_Found", "can't find \"jar\" command");
    }
    
    # space before value, new line
    writeFile("$BuildRoot1/MANIFEST.MF", "Implementation-Version: 1.0\n");
    writeFile("$BuildRoot2/MANIFEST.MF", "Implementation-Version: 2.0\n");
    
    my (%SrcDir1, %SrcDir2) = ();
    foreach my $Path (cmdFind($BuildRoot1, "f", "*\\.java")) {
        $SrcDir1{getDirname($Path)} = 1;
    }
    foreach my $Path (cmdFind($BuildRoot2, "f", "*\\.java")) {
        $SrcDir2{getDirname($Path)} = 1;
    }
    # build classes v.1
    foreach my $Dir (keys(%SrcDir1))
    {
        chdir($Dir);
        system("$JavacCmd -g *.java");
        chdir($In::Opt{"OrigDir"});
        if($?) {
            exitStatus("Error", "can't compile classes v.1");
        }
    }
    # create java archive v.1
    chdir($BuildRoot1);
    system("$JarCmd -cmf MANIFEST.MF $LibName.jar TestPackage");
    chdir($In::Opt{"OrigDir"});
    
    # build classes v.2
    foreach my $Dir (keys(%SrcDir2))
    {
        chdir($Dir);
        system("$JavacCmd -g *.java");
        chdir($In::Opt{"OrigDir"});
        if($?) {
            exitStatus("Error", "can't compile classes v.2");
        }
    }
    # create java archive v.2
    chdir($BuildRoot2);
    system("$JarCmd -cmf MANIFEST.MF $LibName.jar TestPackage");
    chdir($In::Opt{"OrigDir"});
    
    foreach my $SrcPath (cmdFind($BuildRoot1, "", "*\\.java")) {
        unlink($SrcPath);
    }
    foreach my $SrcPath (cmdFind($BuildRoot2, "", "*\\.java")) {
        unlink($SrcPath);
    }
    return 1;
}

sub runChecker($$$)
{
    my ($LibName, $Path1, $Path2) = @_;
    
    writeFile("$LibName/v1.xml", "
        <version>
            1.0
        </version>
        <archives>
            ".getAbsPath($Path1)."
        </archives>\n");
    
    writeFile("$LibName/v2.xml", "
        <version>
            2.0
        </version>
        <archives>
            ".getAbsPath($Path2)."
        </archives>\n");
    
    my $Cmd = "perl $0 -l $LibName $LibName/v1.xml $LibName/v2.xml";
    if($In::Opt{"Quick"}) {
        $Cmd .= " -quick";
    }
    if(defined $In::Opt{"SkipDeprecated"}) {
        $Cmd .= " -skip-deprecated";
    }
    if(defined $In::Opt{"OldStyle"}) {
        $Cmd .= " -old-style";
    }
    
    my $TmpDir = $In::Opt{"Tmp"};
    
    writeFile($TmpDir."/skip-annotations.list", "TestPackage.Beta");
    $Cmd .= " -skip-annotations-list ".$TmpDir."/skip-annotations.list";
    if($In::Opt{"Debug"})
    {
        $Cmd .= " -debug";
        printMsg("INFO", "Executing $Cmd");
    }
    
    my $Report = "compat_reports/$LibName/1.0_to_2.0/compat_report.html";
    
    if(-f $Report) {
        unlink($Report);
    }
    
    system($Cmd);
    
    if(not -f $Report) {
        exitStatus("Error", "analysis has failed");
    }
    
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
        printMsg("INFO", "Test result: SUCCESS ($NProblems breaks found)\n");
    }
    else {
        printMsg("ERROR", "Test result: FAILED ($NProblems breaks found)\n");
    }
}

return 1;

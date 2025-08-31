class TC01 {
    public static void main(String[] args) {
        A o;
        int res;
        o = new A();
        res = o.foo(); 
        System.out.println(res);
    }
}

class A {
    public int foo() {
        int a;
        B o1;
        C o2; // created to store the typecasted object
        o1 = new C();
        o2 = (C)o1; // Typecast the receiver object to C
        a = C.bar(o2); // Call the static method bar of C
        return a;
    }
}
class B {
    int f;
    public int bar() {
        int t1;
        t1 = 1;
        System.out.println(f);
        return t1;
    }
}
class C extends B {
    int f;
    public int bar() {
        C t1;
        int t2;
        t1 = this;
        t2 = t1.f;
        System.out.println(t2);
        return t2;
    }
    // Newly created static method "C:bar"
    public static int bar(C z) {
        int t1;
        t1 = z.f;
        System.out.println(t1);
        return t1;
    }
}

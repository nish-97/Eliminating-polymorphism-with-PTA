class TC01 {
    // Main class: Just to call foo() 
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
        o1 = new C(); // PTA of O1 = {O16)
        a = o1.bar(); // PTA of O1 = {O16} --> Monomorphic call to C:bar
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
}

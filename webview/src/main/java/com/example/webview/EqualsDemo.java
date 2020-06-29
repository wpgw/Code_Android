package com.example.webview;

public class EqualsDemo {
    String b;
    public EqualsDemo(String i){
        this.b=i;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(!(obj instanceof EqualsDemo))
            return false;
        EqualsDemo other = (EqualsDemo)obj;
        if(b == null){
            if(other.b !=null)
                return false;
        }else if(b.equals(other.b))
            return true;
        return false;
    }

    public static void main(String[] args) {

        EqualsDemo a = new EqualsDemo("1");
        EqualsDemo b = new EqualsDemo("1");
        EqualsDemo d = new EqualsDemo("2");
        EqualsDemo c = a;

        System.out.println(a==c);
        System.out.println(a==d);
        System.out.println(b==c);
        System.out.println(b==a);

        System.out.println(a.equals(c));
        System.out.println(a.equals(d));
        System.out.println(b.equals(c));
        System.out.println(b.equals(a));

        System.out.println("null==null"+null==null);
    }
}

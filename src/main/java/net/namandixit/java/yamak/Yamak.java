package net.namandixit.java.yamak;

public class Yamak {

    public static void main(String[] args) {
        Compiler compiler = new Compiler();
        compiler.runString("", "true false 23.34 hello bye func lambda 0x1.400p3 0x10.1p0 123.456p-67 #{Commenting #{nested {{{ #} }}} #} #byeeeee 420");
    }
}

del Util.class
del Mineshafter-proxy.jar
javac Util.java
jar cfe Mineshafter-proxy.jar mineshafter.MineClient Util.class manifest.txt -C bin/ mineshafter/
pause
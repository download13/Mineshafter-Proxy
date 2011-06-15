del Util.class AllowAll.class
del Mineshafter-proxy.jar
javac Util.java AllowAll.java
jar cfe Mineshafter-proxy.jar mineshafter.MineClient AllowAll.class Util.class manifest.txt keys.jks auth -C bin/ mineshafter/
pause
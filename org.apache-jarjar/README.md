# org.apache-jarjar

This project provides a JAR of apache libraries with the package translated
from `org.apache` to `io.nextop.org.apache` using jarjar. This fixes package
conflicts with the Android base where the base version is used instead of the bundled version.

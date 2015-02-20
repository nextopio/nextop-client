package io.nextop.org.apache.http.impl.execchain;

import io.nextop.org.apache.commons.logging.Log;

public class NextopLogger {

    static final Log EMPTY = new Log() {
        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public boolean isFatalEnabled() {
            return false;
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public void trace(Object o) {

        }

        @Override
        public void trace(Object o, Throwable throwable) {

        }

        @Override
        public void debug(Object o) {

        }

        @Override
        public void debug(Object o, Throwable throwable) {

        }

        @Override
        public void info(Object o) {

        }

        @Override
        public void info(Object o, Throwable throwable) {

        }

        @Override
        public void warn(Object o) {

        }

        @Override
        public void warn(Object o, Throwable throwable) {

        }

        @Override
        public void error(Object o) {

        }

        @Override
        public void error(Object o, Throwable throwable) {

        }

        @Override
        public void fatal(Object o) {

        }

        @Override
        public void fatal(Object o, Throwable throwable) {

        }
    };
}

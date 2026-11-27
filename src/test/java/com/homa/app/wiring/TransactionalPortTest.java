package com.homa.app.wiring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class TransactionalPortTest {

    interface Greeter {
        String greet(String name);
    }

    static final class RefusedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RefusedException(String message) {
            super(message);
        }
    }

    /** Records begin, commit and rollback without a database. */
    static final class RecordingTransactions extends AbstractPlatformTransactionManager {
        private static final long serialVersionUID = 1L;
        final List<String> log = new ArrayList<>();

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            log.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            log.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            log.add("rollback");
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return false;
        }

        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            log.add("rollback-only");
        }

    }

    private final RecordingTransactions transactions = new RecordingTransactions();

    @Test
    @DisplayName("A use case call runs in one transaction that commits when it returns.")
    void callCommitsWhenTheUseCaseReturns() {
        Greeter greeter = TransactionalPort.wrap(Greeter.class, name -> "Hello " + name, transactions);

        assertThat(greeter.greet("Aisyah")).isEqualTo("Hello Aisyah");
        assertThat(transactions.log).containsExactly("begin", "commit");
    }

    @Test
    @DisplayName("A refusal rolls the transaction back and reaches the caller as itself.")
    void refusalRollsBackAndIsRethrownUnchanged() {
        Greeter greeter = TransactionalPort.wrap(Greeter.class, name -> {
            throw new RefusedException("CUS-7: taken");
        }, transactions);

        assertThatThrownBy(() -> greeter.greet("Daniel"))
                .isExactlyInstanceOf(RefusedException.class)
                .hasMessage("CUS-7: taken");
        assertThat(transactions.log).containsExactly("begin", "rollback");
    }

    @Test
    @DisplayName("Object methods such as toString are not run in a transaction.")
    void objectMethodsRunWithoutATransaction() {
        Greeter target = new Greeter() {
            @Override
            public String greet(String name) {
                return name;
            }

            @Override
            public String toString() {
                return "greeter";
            }
        };
        Greeter greeter = TransactionalPort.wrap(Greeter.class, target, transactions);

        assertThat(greeter.toString()).isEqualTo("greeter");
        assertThat(transactions.log).isEmpty();
    }

    @Test
    @DisplayName("Only a port interface can be wrapped, and it needs a target.")
    void onlyAnInterfaceWithATargetCanBeWrapped() {
        assertThatThrownBy(() -> TransactionalPort.wrap(String.class, "x", transactions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("java.lang.String");
        assertThatThrownBy(() -> TransactionalPort.wrap(Greeter.class, null, transactions))
                .isInstanceOf(NullPointerException.class);
    }
}

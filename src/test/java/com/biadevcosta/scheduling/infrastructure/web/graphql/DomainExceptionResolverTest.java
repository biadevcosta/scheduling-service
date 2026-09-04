package com.biadevcosta.scheduling.infrastructure.web.graphql;

import com.biadevcosta.scheduling.domain.exception.AppointmentNotFoundException;
import com.biadevcosta.scheduling.domain.exception.InvalidAppointmentException;
import com.biadevcosta.scheduling.domain.exception.NotAppointmentOwnerException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionResolverTest {

    private final DomainExceptionResolver resolver = new DomainExceptionResolver();
    private final DataFetchingEnvironment env = DataFetchingEnvironmentImpl
            .newDataFetchingEnvironment().build();

    private GraphQLError resolve(Throwable ex) {
        return resolver.resolveToSingleError(ex, env);
    }

    @Test
    void ownershipViolation_mapsToForbidden() {
        GraphQLError error = resolve(new NotAppointmentOwnerException());
        assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void notFound_mapsToNotFound() {
        GraphQLError error = resolve(new AppointmentNotFoundException("x"));
        assertThat(error.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @Test
    void invalid_mapsToBadRequest() {
        GraphQLError error = resolve(new InvalidAppointmentException("bad"));
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getMessage()).isEqualTo("bad");
    }

    @Test
    void unknownException_isNotResolvedHere() {
        assertThat(resolve(new IllegalStateException("boom"))).isNull();
    }
}

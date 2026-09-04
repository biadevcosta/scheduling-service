package com.biadevcosta.scheduling.infrastructure.web.graphql;

import com.biadevcosta.scheduling.domain.exception.AppointmentNotFoundException;
import com.biadevcosta.scheduling.domain.exception.InvalidAppointmentException;
import com.biadevcosta.scheduling.domain.exception.NotAppointmentOwnerException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/** Maps domain exceptions to GraphQL error types instead of leaking a raw 500. */
@Component
public class DomainExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType type = switch (ex) {
            case NotAppointmentOwnerException ignored -> ErrorType.FORBIDDEN;
            case AppointmentNotFoundException ignored -> ErrorType.NOT_FOUND;
            case InvalidAppointmentException ignored -> ErrorType.BAD_REQUEST;
            default -> null;
        };
        if (type == null) {
            return null; // let Spring handle everything else
        }
        return GraphqlErrorBuilder.newError()
                .errorType(type)
                .message(ex.getMessage())
                .build();
    }
}

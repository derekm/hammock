/*
 * Copyright 2017 Hammock and its contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.ament.hammock.jwt.bean;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import ws.ament.hammock.jwt.JWTPrincipal;
import ws.ament.hammock.utils.BiFunctionBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

public class JWTExtension implements Extension {

    private static final Predicate<InjectionPoint> NOT_PROVIDERS = ip -> (ip.getType() instanceof Class) || (ip.getType() instanceof ParameterizedType && ((ParameterizedType) ip.getType()).getRawType() != Provider.class);

    private Set<InjectionPoint> injectionPoints = new HashSet<>();

    public void locateClaims(@Observes ProcessInjectionPoint<?, ?> pip) {
        Claim claim = pip.getInjectionPoint().getAnnotated().getAnnotation(Claim.class);
        if (claim != null) {
            injectionPoints.add(pip.getInjectionPoint());
        }
    }

    public void registerConfigProducer(@Observes AfterBeanDiscovery abd) {
        Set<ClaimDefinition> claimDefinitions = injectionPoints.stream()
                .filter(NOT_PROVIDERS)
                .map(JWTExtension::toDefinition)
                .collect(Collectors.toSet());

        Set<ClaimDefinition> providers = injectionPoints.stream()
                .filter(NOT_PROVIDERS.negate())
                .map(JWTExtension::toDefinition)
                .collect(Collectors.toSet());

        claimDefinitions.addAll(providers);

        claimDefinitions.forEach(claimDefinition -> {
            abd.addBean(new BiFunctionBean<>(BiFunctionBean.class, claimDefinition.returnType,
                    claimDefinition, singleton(claimDefinition.claim),
                    Dependent.class, ClaimProducer.INSTANCE));
        });
    }

    private static ClaimDefinition toDefinition(InjectionPoint ip) {
        return createClaimDefinition(ip.getType(), ip);
    }

    private static ClaimDefinition createClaimDefinition(Type returnType, InjectionPoint ip) {
        if(returnType instanceof ParameterizedType && ((ParameterizedType) returnType).getRawType() == Provider.class) {
            returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
        }
        Type typeArgument = (returnType instanceof ParameterizedType) ?
                ((ParameterizedType) returnType).getActualTypeArguments()[0] : returnType;
        return new ClaimDefinition(getClaim(ip.getQualifiers()), returnType, typeArgument);
    }

    private static Claim getClaim(Set<Annotation> annotations) {
        for (Annotation a : annotations) {
            if (a instanceof Claim) {
                return (Claim) a;
            }
        }
        return null;
    }

    private static final class ClaimProducer implements BiFunction<CreationalContext<Object>,
            ClaimDefinition, Object> {
        static ClaimProducer INSTANCE = new ClaimProducer();

        @Override
        public Object apply(CreationalContext<Object> cc, ClaimDefinition claimDefinition) {
            JWTPrincipal jwtPrincipal = CDI.current().select(JWTPrincipal.class).get();
            HammockClaimValue value = new HammockClaimValue<>(jwtPrincipal, claimDefinition);
            if (claimDefinition.returnType instanceof ParameterizedType &&
                    ((Class)((ParameterizedType)claimDefinition.returnType).getRawType()).isAssignableFrom(ClaimValue.class)) {
                return value;
            }
            else {
                return value.getValue();
            }
        }
    }

}

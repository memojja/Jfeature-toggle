package org.flips.store;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.flips.annotation.Flips;
import org.flips.model.AnnotationMetaData;
import org.flips.model.FeatureFlipAnnotationMetaDataFactory;
import org.flips.processor.FeatureFlipAnnotationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class FeatureFlipAnnotationMetaDataStore {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlipAnnotationMetaDataStore.class);

    private Map<Method, AnnotationMetaData> methodFeatureFlipAnnotationMetaDataStore = new HashMap<>();

    private ApplicationContext                      applicationContext;
    private FeatureFlipAnnotationProcessor          featureFlipAnnotationProcessor;
    private FeatureFlipAnnotationMetaDataFactory    featureFlipAnnotationMetaDataFactory;

    @Autowired
    public FeatureFlipAnnotationMetaDataStore(ApplicationContext                    applicationContext,
                                              FeatureFlipAnnotationProcessor        featureFlipAnnotationProcessor,
                                              FeatureFlipAnnotationMetaDataFactory  featureFlipAnnotationMetaDataFactory) {

        this.applicationContext                     = applicationContext;
        this.featureFlipAnnotationProcessor         = featureFlipAnnotationProcessor;
        this.featureFlipAnnotationMetaDataFactory   = featureFlipAnnotationMetaDataFactory;
    }

    @PostConstruct
    protected void buildFeatureFlipAnnotationMetaDataStore(){
        Map<String, Object> flipComponents = getFlipComponents();
        if ( !flipComponents.isEmpty() ) {
            flipComponents.entrySet().stream().forEach(entry -> storeMethodWithFeatureFlipAnnotationMetaData(AopProxyUtils.ultimateTargetClass(entry.getValue())));
        }
        logger.info("Completed building FeatureFlipAnnotationMetaDataStore {}", methodFeatureFlipAnnotationMetaDataStore);
    }

    public boolean isFeatureEnabled(Method method) {
        return methodFeatureFlipAnnotationMetaDataStore
                .getOrDefault(method, featureFlipAnnotationMetaDataFactory.getEmptyAnnotationMetaData())
                .evaluate();
    }

    public int getTotalMethodsCached(){
        return methodFeatureFlipAnnotationMetaDataStore.size();
    }

    //return a copy ?
    public Set<Method> allMethodsCached(){
        return methodFeatureFlipAnnotationMetaDataStore.keySet();
    }

    private void storeMethodWithFeatureFlipAnnotationMetaData(Class<?> clazz){
        Method[] methods = clazz.getDeclaredMethods();

        for( Method method : methods ){
            Method accessibleMethod = MethodUtils.getAccessibleMethod(method);
            if ( accessibleMethod != null ){
                AnnotationMetaData annotationMetaData = featureFlipAnnotationProcessor.getAnnotationMetaData(accessibleMethod);
                if ( !annotationMetaData.isEmpty() ) {
                    logger.debug("Evaluated method accessibility {} to put in FeatureFlipAnnotationMetaDataStore as {}", method.getName(), annotationMetaData.toString());
                    methodFeatureFlipAnnotationMetaDataStore.put(method, annotationMetaData);
                }
            }
        }
    }

    private Map<String, Object> getFlipComponents() {
        return applicationContext.getBeansWithAnnotation(Flips.class);
    }
}
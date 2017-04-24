package com.ethlo.dachs.hibernate;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.ethlo.dachs.CollectingEntityChangeListener;
import com.ethlo.dachs.CollectingEntityChangeSetListener;
import com.ethlo.dachs.EntityChangeListener;
import com.ethlo.dachs.EntityChangeSetListener;
import com.ethlo.dachs.EntityListenerIgnore;
import com.ethlo.dachs.InternalEntityListener;
import com.ethlo.dachs.jpa.DefaultInternalEntityListener;
import com.ethlo.dachs.jpa.NotifyingJpaTransactionManager;
import com.ethlo.dachs.test.model.Customer;
import com.ethlo.dachs.test.repository.CustomerRepository;

@SpringBootApplication
@EnableAutoConfiguration
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses=CustomerRepository.class)
@EntityScan(basePackageClasses=Customer.class)
public class HibernateCfg extends JpaBaseConfiguration
{
    protected HibernateCfg(DataSource dataSource, JpaProperties properties, ObjectProvider<JtaTransactionManager> jtaTransactionManager,
                    ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers)
    {
        super(dataSource, properties, jtaTransactionManager, transactionManagerCustomizers);
    }

	@Bean
	public CollectingEntityChangeSetListener collectingSetListener()
	{
		return new CollectingEntityChangeSetListener();
	}
	
	@Bean
	public CollectingEntityChangeListener collectingListener()
	{
		return new CollectingEntityChangeListener();
	}
	
	@Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter()
	{
		return new HibernateJpaVendorAdapter();
	}
	
    @Bean
    public static JpaTransactionManager transactionManager(InternalEntityListener internalEntityListener)
    {
        return new NotifyingJpaTransactionManager(internalEntityListener);
    }
	
	@Bean
	public static InternalEntityListener internalEntityListener(EntityManagerFactory emf, EntityChangeSetListener txnBoundListener, EntityChangeListener directListener)
	{
	    final InternalEntityListener internalEntityListener = new DefaultInternalEntityListener(emf, Arrays.asList(txnBoundListener), Arrays.asList(directListener))
		    .setLazyIdExtractor(new HibernateLazyIdExtractor(emf))
            .fieldFilter(f->
            {
                return !Modifier.isStatic(f.getModifiers()) 
                       && f.getDeclaredAnnotation(EntityListenerIgnore.class) == null;
            });
	    
	    HibernatePropertyChangeInterceptorBridge.setHibernatePropertyChangeInterceptor(new HibernatePropertyChangeInterceptor(internalEntityListener));
	    
	    return internalEntityListener;
	}

	@Override
	protected Map<String, Object> getVendorProperties()
	{
		final Map<String, Object> retVal = new TreeMap<>();
		
		// Connecting the listener to Dachs
		retVal.put("hibernate.ejb.interceptor", new HibernatePropertyChangeInterceptorBridge());
		retVal.put("hibernate.hbm2ddl.auto", "create");
		return retVal;
	}
}

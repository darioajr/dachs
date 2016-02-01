# Dachs - Data Change Snitch

### Goal

To have a _unified_ API to listen for core data changes that is stable, efficient and non-obtrusive.

### Rationale

In almost all back-end systems there is the need to notify or update additional data whenever core data changes. It can be sending a notification event, invalidating cache entries, audit logging or updating a search engine to mention a few. 

For example Spring has already support for events, and from [Spring 4.2](https://spring.io/blog/2015/02/11/better-application-events-in-spring-framework-4-2#transaction-bound-events) it also has support for send-at-end-of-transaction-events. Further Spring JPA supports [auditing](http://docs.spring.io/spring-data/jpa/docs/1.5.0.RELEASE/reference/html/jpa.repositories.html#jpa.auditing), however it does not support fetching the actual data, just _who_ changed it and _when_. There is no recollection of _what_ was changed.

![Dachs flow](/resources/dachs_flow.png)

All the different persistence frameworks have their different APIs for detecting data changes. With Dachs you have one simple, unified API to deal with.

### Maven artifact
```xml
<dependency>
  <groupId>com.ethlo.dachs</groupId>
  <artifactId>dachs-{impl}</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### Status
[![Build Status](https://travis-ci.org/ethlo/dachs.png?branch=master)](https://travis-ci.org/ethlo/dachs)

### Supported persistence frameworks
* Eclipselink - [Guide](dachs-eclipselink/README.md)
* Hibernate - [Guide](dachs-hibernate/README.md)

### API
The goal is to have a simple, but powerful API to get notifications of all changes to entities, that is `created`, `updated` and `deleted`.

```java
public interface EntityListener
{
	void created(EntityData entityData);
	void updated(EntityData entityData);
	void deleted(EntityData entityData);
}
```

Using this simple listener, we get an `EntityData` object for each operation on the entity.

```java
public interface EntityData
{

	/**
	 * Returns the id of the entity
	 * @return the id of the entity
	 */
	Serializable getId();

	/**
	 * Returns the entity
	 * @return The entity
	 */
	Object getEntity();

	/**
	 * Get all propertyChanges
	 * @return A list of all property changes for this entity
	 */
	Collection<PropertyChange<?>> getPropertyChanges();

	/**
	 * Get a {@link PropertyChange} for the given propertyName of this entity
	 * @param propertyName The name of the entity property
	 * @return The {@link PropertyChange} for the given propertyName
	 */
	Optional<PropertyChange<?>> getPropertyChange(String propertyName);
```

Each `EntityData` object holds a collection of `PropertyChanges` that is the individual properties that has changed.

```java
public class PropertyChange<T>
{
	public String getPropertyName()
	{
		return propertyName;
	}

	public Class<T> getEntityType()
	{
		return entityType;
	}

	public T getOldValue()
	{
		return oldValue;
	}

	public T getNewValue()
	{
		return newValue;
	}
}
```

####Example output

Given a simple Person object:

```java
public class Person()
{
	private String name;
	private Integer age;
}
```

#####Created
```
EntityData
	propertyChanges:
		* name - null => "John Doe"
		* age - null => 34
```

#####Updated
```
EntityData
	propertyChanges:
		* name - "John Doe" => "John Smith"
		* age - 34 => 47
```

#####Deleted
```
EntityData
	propertyChanges:
		* name - "John Smith" => null
		* age - 47 => null
```

### Transaction boundaries (if applicable)
Often we do not care for events before they are actually committed. For example, we do not want to store audit data if the transaction was rolled back. Dachs can buffer events until commit if in a transactional context.

### Limitations
Dachs relies on the persistence framework in use to notify about operations and there might be limitations. 
In general bulk delete will not trigger delete events (aka `DELETE FROM Entity`). 

### Performance
Simple tests indicate that you can expect about 1-5% degradation in performance with Dachs enabled. YMMV.

NOTE: Keep in mind that this test was performed on an in-memory database, performing 20 000 calls in about 2.5 seconds. Normally the real overhead would be negligible as the performance hit of your database would far outweigh this. 

### Release history
TBA

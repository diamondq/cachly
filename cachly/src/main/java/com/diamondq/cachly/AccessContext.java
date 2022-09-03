package com.diamondq.cachly;

import java.util.Map;

/**
 * An AccessContext provides additional information to refine queries and lookups, such as authentication
 */
public interface AccessContext
{

	/**
	 * Returns the data from the AccessContext
	 *
	 * @return the data map
	 */
	Map<Class<?>, Object> getData();

}

package com.argot.remote.command;

import java.lang.reflect.Method;
import java.util.HashMap;


import com.argot.TypeElement;
import com.argot.TypeException;
import com.argot.TypeLibrary;
import com.argot.TypeLocation;
import com.argot.TypeLocationName;

import com.argot.meta.MetaAbstract;
import com.argot.meta.MetaIdentity;
import com.argot.meta.MetaMarshaller;
import com.argot.meta.MetaName;
import com.argot.meta.MetaReference;
import com.argot.meta.MetaSequence;
import com.argot.meta.MetaTag;

public class MetaCommand 
extends MetaMarshaller
{

	private HashMap<Method,MetaSequence> _methodToMetaMethod;
	private HashMap<MetaSequence,Method> _metaMethodToMethod;
	private MetaReference[] _references;

	public MetaCommand()
	{
		_methodToMetaMethod = new HashMap<Method,MetaSequence>();
		_metaMethodToMethod = new HashMap<MetaSequence,Method>();
	}
	
	public Method getMethod(int id)
	{
		return _metaMethodToMethod.get(id);
	}
	
	public MetaSequence getTypeId(Method m)
	{
		return _methodToMetaMethod.get(m);
	}
	
	public MetaReference[] getReferences()
	{
		return _references;
	}
	
	@Override
	public void bind(TypeLibrary library, int memberTypeId, TypeElement definition) 
	throws TypeException 
	{
		super.bind(library, memberTypeId, definition);
		
		// bind the java methods to MetaMethods.
		// If no class is bound then do nothing.
		Class<?> clss;
		try 
		{
			clss = library.getClass(memberTypeId);
			if (!clss.isInterface())
			{
				throw new TypeException("MetaCommand: Class is not interface: " + clss.getName() );
			}
		} 
		catch (TypeException e) 
		{
			// DO NOTHING.
			return;
		}
		bindMethods( library, (MetaAbstract) definition, clss );
	}

	
	private void bindMethods( TypeLibrary library, MetaAbstract metaAbstract, Class<?> clss ) 
	throws TypeException
	{
		Integer[] ids = metaAbstract.getConcreteIds();
		for ( int x=0; x< ids.length; x++ )
		{
			int id = ids[x];
			TypeElement structure = library.getStructure(id);
			if (!(structure instanceof MetaIdentity))
			{
				throw new TypeException("Failed to Bind Command:" + library.getName(id) );
			}
			MetaIdentity identity = (MetaIdentity) structure;
			
			
			TypeLocation location = library.getLocation(id);
			if (!(location instanceof TypeLocationName))
			{
				throw new TypeException("Failed to Bind Command:" + library.getName(id) );
			}
			TypeLocationName locationName = (TypeLocationName) location;
			
			Integer[] definitionIds = identity.getVersionIdentifiers();
			for (int y=0; y<definitionIds.length;y++)
			{
				TypeElement sequenceElement = library.getStructure(definitionIds[y]);
				if (!(sequenceElement instanceof MetaSequence))
				{
					throw new TypeException("Failed to Bind Command:" + library.getName(id) );
				}
				MetaSequence sequence = (MetaSequence) sequenceElement;
				
				try {
					Method method = findMethod( library, clss, locationName.getName().getName(), (MetaSequence) sequenceElement );
					_methodToMetaMethod.put( method, sequence );
					_metaMethodToMethod.put( sequence, method );
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("WARNING: " +  locationName.getName().getFullName() + " " + e.getMessage());
				}
			}
			
			
		}
	}
	
	private MetaReference[] getReferences(MetaSequence sequence) 
	throws TypeException
	{
		MetaReference[] references = new MetaReference[sequence.size()];
		
		for (int x=0; x<sequence.size();x++)
		{
			TypeElement e = sequence.getElement(x);
			if (e instanceof MetaReference)
			{
				references[x] = (MetaReference) e;
				continue;
			}
			else if (e instanceof MetaTag)
			{
				TypeElement te = ((MetaTag)e).getExpression();
				if (te instanceof MetaReference)
				{
					references[x] = (MetaReference) te;
					continue;
				}
				throw new TypeException("Failed to bind. Unknown tag " + ((MetaTag)e).getDescription() );
			}
			throw new TypeException("Failed to bind. Sequence must be references or tagged references.");
		}
		
		return references;
	}
	
	/**
	 * A helper class used to proces a request and invoke a proxy
	 * object.
	 * 
	 * @param request
	 * @throws SomException
	 */
	private Method findMethod( TypeLibrary library, Class<?> proxyClass, String name, MetaSequence sequence )
	throws TypeException
	{
		MetaReference[] requestTypes = getReferences(sequence);
		
		// Setup the objects used to invoke the request.
		Class<?>[] args = new Class[ sequence.size()];
		
		// Pop the args back into the Arrays from the Request.
		for ( int x = 0 ; x < sequence.size() ; x++ )
		{
			try
			{
				TypeElement element = library.getStructure(requestTypes[x].getType());
				if (!(element instanceof MetaIdentity))
				{
					throw new TypeException("MetaCommand: Failed to bind method: " + name + " arg: " + (x+1) + " not referencing identity.");
				}
				MetaIdentity identity = (MetaIdentity)element;
				Integer[] ids = identity.getVersionIdentifiers();
				if (ids.length == 0)
				{
					throw new TypeException("MetaCommand: Failed to bind method: " + name + " arg: " + (x+1) + " has not definitions.");	
				}
				args[x] = library.getClass( ids[0] );
			}
			catch( TypeException e)
			{
				MetaName filedType = library.getName(requestTypes[x].getType());
				throw new TypeException("Failed to bind method: " + name + " arg: " + (x+1) + " " + filedType.getFullName(), e);
			}
		}
		
		Method method = null;		
		try
		{
			// Using the method name and classes, get the method.
			//System.out.println( "Invoke " + request.method() + " argCount:" + cargs.length );
			
			// Can't use getMethod because it does not handle conversion of
			// basic types correctly.  We need to filter out the basic types
			// and match primitives with java object types we're supplied.
			//method = _proxyClass.getMethod( request.method(), cargs );
			
			
			// Search through each of the methods on the object.
			Method[] methods = proxyClass.getMethods();
			for ( int y = 0 ; y < methods.length ; y++ )
			{
				
				// First find the right name.
				if ( !methods[y].getName().equals( name ))
					continue;
					
				// Now check we have the same parameter length
				Class<?>[] paramTypes = methods[y].getParameterTypes();
				if ( paramTypes.length != requestTypes.length )
					continue;

				method = methods[y];
				
				
				// Now check the param types are the same.
				boolean found = true;
				for ( int z = 0; z< paramTypes.length; z++ )
				{
					// If this fails do some further checking.
					//if ( !paramTypes[z].isInstance( args[z] ) )
					if (!paramTypes[z].equals(args[z]))
					{
						if ( args[z] == null )
							continue;
						
						// First check if we have any basic types.
						if ( paramTypes[z].getName().equals( "short") && args[z].getName().equals( "java.lang.Short") )
							continue;
											
						if ( paramTypes[z].getName().equals( "byte" ) && args[z].getName().equals( "java.lang.Byte"))
							continue;
					
						if ( paramTypes[z].getName().equals( "int" ) && args[z].getName().equals( "java.lang.Integer" ))
							continue;
						
						if ( paramTypes[z].getName().equals( "long" ) && args[z].getName().equals( "java.lang.Long" ))
							continue;
						
						if ( paramTypes[z].getName().equals( "boolean") && args[z].getName().equals( "java.lang.Boolean" ))
							continue;
											
						found = false;
						break;
						
					}
				}
				
				// If we got through all the params break.  We got it.
				if ( found )
					break;
				
				method = null;
			}
		}
		catch (SecurityException e)
		{
			throw new TypeException( "SecurityException", e );
		}

		if ( method == null )
		{
			StringBuffer error = new StringBuffer();
			error.append(proxyClass.getName());
			error.append(".");
			error.append(name);
			error.append("(");
			for ( int x=0; x < args.length; x++ )
			{
				if ( args[x] != null)
					error.append( args[x].getName() );
				else
					error.append( "null");
				
				if ( x < args.length-1 )
					error.append( "," );
			}
			error.append(")");
						
			throw new TypeException( "NoSuchMethod '" + error + "'" );
		}	
		return method;
	}
	
}

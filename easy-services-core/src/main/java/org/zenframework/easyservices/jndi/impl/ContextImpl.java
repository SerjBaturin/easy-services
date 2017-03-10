package org.zenframework.easyservices.jndi.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;

public class ContextImpl implements Context {

    private static final NameParser NAME_PARSER = new NameParser() {

        @Override
        public Name parse(String name) throws NamingException {
            return new NameImpl(name);
        }

    };

    private final String name;
    private final String prefix;
    private final Map<String, Binding> context;
    private final Hashtable<String, Object> environment = new Hashtable<String, Object>();

    public ContextImpl() {
        this("", new HashMap<String, Binding>());
    }

    private ContextImpl(String name, Map<String, Binding> context) {
        this.name = name;
        this.prefix = name.isEmpty() ? "" : name + NameImpl.DELIMETER;
        this.context = context;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException {
        name = prefix + name;
        Object result;
        synchronized (context) {
            Binding binding = context.get(name);
            if (binding == null)
                throw new NameNotFoundException("Object '" + name + "' not bound");
            result = binding.getObject();
        }
        if (result instanceof LinkRef) {
            result = lookup(((LinkRef) result).getLinkName());
        } else if (result instanceof Reference) {
            try {
                result = NamingManager.getObjectInstance(result, null, null, this.environment);
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                throw (NamingException) new NamingException("Could not look up : " + name).initCause(e);
            }
        }
        return result;
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        bind(name.toString(), obj);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        name = prefix + name;
        synchronized (context) {
            if (context.containsKey(name))
                throw new NameAlreadyBoundException("Object '" + name + "' already bound");
            context.put(name, new Binding(name, obj));
        }
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        rebind(name.toString(), obj);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        name = prefix + name;
        synchronized (context) {
            context.put(name, new Binding(name, obj));
        }
    }

    @Override
    public void unbind(Name name) throws NamingException {
        unbind(name.toString());
    }

    @Override
    public void unbind(String name) throws NamingException {
        name = prefix + name;
        synchronized (context) {
            if (!context.containsKey(name))
                throw new NameNotFoundException("Object '" + name + "' not bound");
            context.remove(name);
        }
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        rename(oldName, newName);
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        oldName = prefix + oldName;
        newName = prefix + newName;
        synchronized (context) {
            if (!context.containsKey(oldName))
                throw new NameNotFoundException("Object '" + oldName + "' not bound");
            if (context.containsKey(newName))
                throw new NameAlreadyBoundException("Object '" + newName + "' already bound");
            context.put(newName, context.remove(oldName));
        }
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        name = prefix + name;
        synchronized (context) {
            return new NamingEnumerationImpl<NameClassPair>(getSubContext(name), new NamingEnumerationImpl.ElementFactory<NameClassPair>() {

                @Override
                public NameClassPair getElement(Entry<String, Binding> entry) {
                    return new NameClassPair(entry.getKey(), entry.getValue().getClassName());
                }

            });
        }
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        name = prefix + name;
        synchronized (context) {
            return new NamingEnumerationImpl<Binding>(getSubContext(name), new NamingEnumerationImpl.ElementFactory<Binding>() {

                @Override
                public Binding getElement(Entry<String, Binding> entry) {
                    return entry.getValue();
                }

            });
        }
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        destroySubcontext(name.toString());
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        name = prefix + name;
        synchronized (context) {
            Collection<String> keys = new HashSet<String>(context.keySet());
            String path = name.isEmpty() ? "" : name + NameImpl.DELIMETER;
            for (String key : keys)
                if (key.equals(name) || key.startsWith(path))
                    context.remove(key);
        }
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name.toString());
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return new ContextImpl(name, context);
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return NAME_PARSER;
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return NAME_PARSER;
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return prefix.addAll(name);
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        StringBuilder str = new StringBuilder(name.length() + prefix.length() + NameImpl.DELIMETER.length());
        str.append(prefix);
        if (!name.isEmpty() && !prefix.isEmpty())
            str.append(NameImpl.DELIMETER);
        str.append(name);
        return str.toString();
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return environment.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return environment.remove(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return environment;
    }

    @Override
    public void close() throws NamingException {}

    @Override
    public String getNameInNamespace() throws NamingException {
        return name;
    }

    private Map<String, Binding> getSubContext(String name) throws NamingException {
        Map<String, Binding> subContext = new HashMap<String, Binding>();
        String path = name.isEmpty() ? "" : name + NameImpl.DELIMETER;
        for (Map.Entry<String, Binding> entry : context.entrySet())
            if (entry.getKey().equals(name) || entry.getKey().startsWith(path))
                subContext.put(entry.getKey(), entry.getValue());
        //if (subContext.isEmpty())
        //    throw new NamingException("Subcontext '" + name + "' is empty");
        return subContext;
    }

}

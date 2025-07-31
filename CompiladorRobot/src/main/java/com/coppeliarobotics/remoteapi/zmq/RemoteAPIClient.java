package com.coppeliarobotics.remoteapi.zmq;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import java.math.BigInteger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import co.nstant.in.cbor.*;
import co.nstant.in.cbor.model.DataItem;
import org.zeromq.*;

public class RemoteAPIClient
{
    public RemoteAPIClient()
    {
        this("localhost", -1);
    }

    public RemoteAPIClient(String host, int rpcPort)
    {
        this(host, rpcPort, -1);
    }

    public RemoteAPIClient(String host, int rpcPort, int cntPort)
    {
        this(host, rpcPort, cntPort, -1);
    }

    public RemoteAPIClient(String host, int rpcPort, int cntPort, int verbose)
    {
        if(rpcPort == -1) rpcPort = 23000;

        this.verbose = verbose;
        if(this.verbose == -1)
        {
            String verboseStr = System.getenv("VERBOSE");
            if(verboseStr != null)
                this.verbose = Integer.parseInt(verboseStr);
            else
                this.verbose = 0;
        }

        this.context = new ZContext(1);
        this.rpcSocket = context.createSocket(SocketType.REQ);
        this.rpcSocket.connect(String.format("tcp://%s:%d", host, rpcPort));
        this.uuid = UUID.randomUUID().toString();
    }

    public void close() throws CborException
    {
        DataItem k_func = convertArg("func"),
                 k_args = convertArg("args"),
                 k_uuid = convertArg("uuid"),
                 k_ver = convertArg("ver");

        co.nstant.in.cbor.model.Map req = new co.nstant.in.cbor.model.Map();
        req.put(k_func, convertArg("_*end*_"));
        req.put(k_args, new co.nstant.in.cbor.model.Array());
        req.put(k_uuid, convertArg(this.uuid));
        req.put(k_ver, convertArg(this.VERSION));
        this.send(req);
        this.recv();
        this.rpcSocket.close();
        this.context.close();
    }

    protected void send(DataItem req) throws CborException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(req);
        byte[] data = baos.toByteArray();
        if(this.verbose >= 3)
            System.out.println("Sending (raw): " + dump(data, verbose >= 4 ? -1 : 16));
        this.rpcSocket.send(data);
    }

    protected DataItem recv() throws CborException
    {
        byte[] data = this.rpcSocket.recv(0);
        if(this.verbose >= 3)
            System.out.println("Received (raw): " + dump(data, verbose >= 4 ? -1 : 16));
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        List<DataItem> dataItems = new CborDecoder(bais).decode();
        return dataItems.get(0);
    }

    public String dump(byte[] data, int limit)
    {
        if(limit < 0)
            limit = Integer.MAX_VALUE;
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < Math.min(limit, data.length); i++)
            sb.append(String.format("%02X ", data[i]));
        if(data.length > limit)
            sb.append(String.format("(%d more)", data.length - limit));
        return sb.toString();
    }

    public List<DataItem> callCbor(String func, List<DataItem> args) throws CborException
    {
        DataItem k_func = convertArg("func"),
                 k_args = convertArg("args"),
                 k_err = convertArg("err"),
                 k_uuid = convertArg("uuid"),
                 k_ver = convertArg("ver"),
                 k_lang = convertArg("lang"),
                 k_ret = convertArg("ret"),
                 k_argsl = convertArg("argsL");

        co.nstant.in.cbor.model.Map req = new co.nstant.in.cbor.model.Map();
        req.put(k_func, convertArg(func));
        co.nstant.in.cbor.model.Array v_args = new co.nstant.in.cbor.model.Array();
        for(int i = 0; i < args.size(); i++)
            v_args.add(args.get(i));
        req.put(k_args, v_args);
        req.put(k_uuid, convertArg(this.uuid));
        req.put(k_ver, convertArg(this.VERSION));
        req.put(k_argsl, convertArg(args.size()));
        req.put(k_lang, convertArg("java"));
        if(this.verbose >= 1)
            System.out.println("Sending: " + req.toString());
        this.send(req);

        co.nstant.in.cbor.model.Map rep = (co.nstant.in.cbor.model.Map)this.recv();
        if(this.verbose >= 1)
            System.out.println("Received: " + rep.toString());

        while(rep.getKeys().contains(k_func))
        {
            List<DataItem> callbackResults = new ArrayList<DataItem>();
            co.nstant.in.cbor.model.UnicodeString respFunc = (co.nstant.in.cbor.model.UnicodeString)rep.get(k_func);
            if(respFunc != null && !"_*wait*_".equals(respFunc.getString()))
            {
                List<DataItem> callbackArgs = ((co.nstant.in.cbor.model.Array)rep.get(k_args)).getDataItems();
                callbackResults = executeCallback(respFunc.getString(), callbackArgs);
            }

            co.nstant.in.cbor.model.Map req2 = new co.nstant.in.cbor.model.Map();
            req2.put(k_func, convertArg("_*executed*_"));
            co.nstant.in.cbor.model.Array v_args1 = new co.nstant.in.cbor.model.Array();
            for(int i = 0; i < callbackResults.size(); i++)
                v_args1.add(callbackResults.get(i));
            req2.put(k_args, v_args1);
            req2.put(k_uuid, convertArg(this.uuid));
            req2.put(k_lang, convertArg("java"));
            req2.put(k_ver, convertArg(this.VERSION));
            req2.put(k_argsl, convertArg(callbackResults.size()));
            if(this.verbose >= 1)
                System.out.println("Sending: " + req2.toString());
            this.send(req2);

            rep = (co.nstant.in.cbor.model.Map)this.recv();
            if(this.verbose >= 1)
                System.out.println("Received: " + rep.toString());
        }

        if(rep.getKeys().contains(k_err))
        {
            DataItem error = rep.get(k_err);
            throw new RuntimeException(error != null ? toString(error) : "Unknown error");
        }

        DataItem ret = rep.get(k_ret);
        if(ret instanceof co.nstant.in.cbor.model.Map)
            return java.util.Collections.emptyList();
        return ((co.nstant.in.cbor.model.Array)ret).getDataItems();
    }

    public void registerCallback(String funcName, Function<Object[], Object[]> callback)
    {
        if(callbacks.containsKey(funcName))
            throw new RuntimeException("A callback is already registered for function: " + funcName);
        callbacks.put(funcName, callback);
    }

    private List<DataItem> executeCallback(String funcName, List<DataItem> args)
    {
        if(callbacks.containsKey(funcName))
        {
            Function<Object[], Object[]> callback = callbacks.get(funcName);
            return convertArgs(callback.apply(toObjects(args).toArray()));
        }
        else
            return new ArrayList<DataItem>();
    }

    public DataItem convertArg(Object arg)
    {
        if(arg instanceof Boolean)
            return new co.nstant.in.cbor.model.SimpleValue(((Boolean)arg) ? co.nstant.in.cbor.model.SimpleValueType.TRUE : co.nstant.in.cbor.model.SimpleValueType.FALSE);
        if(arg instanceof String)
            return new co.nstant.in.cbor.model.UnicodeString((String)arg);
        if(arg instanceof Integer)
            return new co.nstant.in.cbor.model.UnsignedInteger(((Integer)arg).longValue());
        if(arg instanceof Long)
            return new co.nstant.in.cbor.model.UnsignedInteger((Long)arg);
        if(arg instanceof Float)
            return new co.nstant.in.cbor.model.SinglePrecisionFloat((Float)arg);
        if(arg instanceof Double)
            return new co.nstant.in.cbor.model.DoublePrecisionFloat((Double)arg);
        if(arg instanceof BigInteger)
            return ((BigInteger)arg).compareTo(BigInteger.ZERO) < 0 ? new co.nstant.in.cbor.model.NegativeInteger((BigInteger)arg) : new co.nstant.in.cbor.model.UnsignedInteger((BigInteger)arg);
        if(arg instanceof List) {
            co.nstant.in.cbor.model.Array array = new co.nstant.in.cbor.model.Array();
            for(Object arg1 : (List)arg)
                array.add(convertArg(arg1));
            return array;
        }
        if(arg instanceof Map)
            throw new RuntimeException("unsupported conversion");
        if(arg.getClass().isArray() && arg.getClass().getComponentType() == byte.class)
            return new co.nstant.in.cbor.model.ByteString((byte[])arg);
        return null;
    }

    public List<DataItem> convertArgs(Object[] inArgs)
    {
        List<DataItem> outArgs = new ArrayList<DataItem>();
        for(int i = 0; i < inArgs.length; i++)
            outArgs.add(convertArg(inArgs[i]));
        return outArgs;
    }

    public Boolean toBoolean(DataItem item)
    {
        co.nstant.in.cbor.model.SimpleValue v = (co.nstant.in.cbor.model.SimpleValue)item;
        co.nstant.in.cbor.model.SimpleValueType t = v.getSimpleValueType();
        if(t == co.nstant.in.cbor.model.SimpleValueType.TRUE) return Boolean.TRUE;
        if(t == co.nstant.in.cbor.model.SimpleValueType.FALSE) return Boolean.FALSE;
        return null;
    }

    public BigInteger toBigInteger(DataItem item)
    {
        return ((co.nstant.in.cbor.model.Number)item).getValue();
    }

    public int toInt(DataItem item)
    {
        return toBigInteger(item).intValue();
    }

    public long toLong(DataItem item)
    {
        return toBigInteger(item).longValue();
    }

    public float toFloat(DataItem item)
    {
        return ((co.nstant.in.cbor.model.AbstractFloat)item).getValue();
    }

    public double toDouble(DataItem item)
    {
        return ((co.nstant.in.cbor.model.DoublePrecisionFloat)item).getValue();
    }

    public String toString(DataItem item)
    {
        return ((co.nstant.in.cbor.model.UnicodeString)item).getString();
    }

    public byte[] toBytes(DataItem item)
    {
        return ((co.nstant.in.cbor.model.ByteString)item).getBytes();
    }

    public Map toMap(DataItem item)
    {
        co.nstant.in.cbor.model.Map m = (co.nstant.in.cbor.model.Map)item;
        Map ret = new HashMap();
        for(DataItem k : m.getKeys())
            ret.put(toObject(k), toObject(m.get(k)));
        return ret;
    }

    public Object toObject(DataItem item)
    {
        if(item instanceof co.nstant.in.cbor.model.SimpleValue) {
            Boolean b = toBoolean(item);
            if(b != null) return b;
        }
        if(item instanceof co.nstant.in.cbor.model.ByteString)
            return toBytes(item);
        if(item instanceof co.nstant.in.cbor.model.AbstractFloat)
            return toFloat(item);
        if(item instanceof co.nstant.in.cbor.model.DoublePrecisionFloat)
            return toDouble(item);
        if(item instanceof co.nstant.in.cbor.model.UnicodeString)
            return toString(item);
        if(item instanceof co.nstant.in.cbor.model.Number)
            return toLong(item);
        if(item instanceof co.nstant.in.cbor.model.Array)
            return toObjects(((co.nstant.in.cbor.model.Array)item).getDataItems());
        if(item instanceof co.nstant.in.cbor.model.Map)
            return toMap(item);
        return null;
    }

    public List<Object> toObjects(List<DataItem> items)
    {
        List<Object> ret = new ArrayList<Object>();
        for(DataItem item : items)
            ret.add(toObject(item));
        return ret;
    }

    public Object[] call(String func, Object... args) throws CborException
    {
        return toObjects(callCbor(func, convertArgs(args))).toArray();
    }

    // ======================================================================
    // === INICIO DE LA MODIFICACIÓN ===
    // ======================================================================

    /**
     * Este método reemplaza al original para no depender de la clase
     * auto-generada 'RemoteAPIObjects'. Ahora crea un objeto genérico.
     */
    public RemoteAPIObject getObject(String name, Map<String, Object> args) {
        return new RemoteAPIObject(this, name, args);
    }

    public RemoteAPIObject getObject(String name) {
        return getObject(name, null);
    }

    // ======================================================================
    // === FIN DE LA MODIFICACIÓN ===
    // ======================================================================
    
    private Map<String, Function<Object[], Object[]>> callbacks = new HashMap<>();
    private int verbose = -1;
    ZContext context;
    ZMQ.Socket rpcSocket;
    String uuid;
    int VERSION = 2;
    // La siguiente línea se elimina o comenta porque causaba el error de compilación.
    // RemoteAPIObjects objs = null;
}
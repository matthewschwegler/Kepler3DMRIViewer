package ptolemy.actor.corba.CoordinatorUtil;

/**
 * ptolemy/actor/corba/CoordinatorUtil/ClientHolder.java .
 * Generated by the IDL-to-Java compiler (portable), version "3.1"
 * from Coordinator.idl
 *
 */

/* A CORBA compatible interface for a consumer.
 */
public final class ClientHolder implements org.omg.CORBA.portable.Streamable {
    public ptolemy.actor.corba.CoordinatorUtil.Client value = null;

    public ClientHolder() {
    }

    public ClientHolder(ptolemy.actor.corba.CoordinatorUtil.Client initialValue) {
        value = initialValue;
    }

    public void _read(org.omg.CORBA.portable.InputStream i) {
        value = ptolemy.actor.corba.CoordinatorUtil.ClientHelper.read(i);
    }

    public void _write(org.omg.CORBA.portable.OutputStream o) {
        ptolemy.actor.corba.CoordinatorUtil.ClientHelper.write(o, value);
    }

    public org.omg.CORBA.TypeCode _type() {
        return ptolemy.actor.corba.CoordinatorUtil.ClientHelper.type();
    }
}
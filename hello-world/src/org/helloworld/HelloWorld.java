package org.helloworld;

import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class HelloWorld extends LimitedFiringSource {

    public PortParameter username;

    public HelloWorld(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);
        output.setTypeEquals(BaseType.STRING);
        username = new PortParameter(this, "username");
        username.setStringMode(true);
    }

    @Override
    public void fire() throws IllegalActionException {
        // TODO Auto-generated method stub
        super.fire();
        username.update();
        String usernameStr = ( (StringToken)username.getToken() ).stringValue();
        output.send(0, new StringToken("Hello " + usernameStr + "!"));
    }
}
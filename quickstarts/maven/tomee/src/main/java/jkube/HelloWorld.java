package jkube;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
@ApplicationScoped

public class HelloWorld {

	@GET
	@Produces({ "text/html" })

	public String getHello() {
		return "Hello World";
	}

}

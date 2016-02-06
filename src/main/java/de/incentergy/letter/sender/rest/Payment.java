package de.incentergy.letter.sender.rest;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;

@Stateless
@Path("/clientPaymentToken")
public class Payment {

	static BraintreeGateway sandbox = new BraintreeGateway(Environment.SANDBOX,
			"ybxr9wjrtmdq4m3b", "nm5fd5nyxc5xhgyf",
			"a3d32191c9bb7311e85ad40bb95ff732");
	static BraintreeGateway production = new BraintreeGateway(
			Environment.PRODUCTION, "khb5qy3qc79swh87", "3hhng5qzx5b4wcpw",
			"93de1a594e7ddd1a78119e7376acec9b");

	public static BraintreeGateway gateway = production;

	@GET
	public String clientPaymentToken() {
		return gateway.clientToken().generate();
	}
}

package ch.ahdis.matchbox;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit4.SpringRunner;

import ch.ahdis.matchbox.spring.boot.autoconfigure.FhirAutoConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class MatchboxApplicationTests {
  
  
	@Test
	public void contextLoads() {

	}


}


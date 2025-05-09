/*
 * SPDX-FileCopyrightText: Copyright © 2016 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.clientsidefiltering;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@RestController
@Slf4j
public class Salaries {

  @Value("${webgoat.user.directory}")
  private String webGoatHomeDirectory;

  @PostConstruct
  public void copyFiles() {
    ClassPathResource classPathResource = new ClassPathResource("lessons/employees.xml");
    File targetDirectory = new File(webGoatHomeDirectory, "/ClientSideFiltering");
    if (!targetDirectory.exists()) {
      targetDirectory.mkdir();
    }
    try {
      FileCopyUtils.copy(
          classPathResource.getInputStream(),
          new FileOutputStream(new File(targetDirectory, "employees.xml")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @GetMapping("clientSideFiltering/salaries")
  @ResponseBody
  public List<Map<String, Object>> invoke() {
    NodeList nodes = null;
    File d = new File(webGoatHomeDirectory, "ClientSideFiltering/employees.xml");
    XPathFactory factory = XPathFactory.newInstance();
    XPath path = factory.newXPath();
    int columns = 5;
    List<Map<String, Object>> json = new ArrayList<>();
    java.util.Map<String, Object> employeeJson = new HashMap<>();

    try (InputStream is = new FileInputStream(d)) {
      InputSource inputSource = new InputSource(is);

      StringBuilder sb = new StringBuilder();

      sb.append("/Employees/Employee/UserID | ");
      sb.append("/Employees/Employee/FirstName | ");
      sb.append("/Employees/Employee/LastName | ");
      sb.append("/Employees/Employee/SSN | ");
      sb.append("/Employees/Employee/Salary ");

      String expression = sb.toString();
      nodes = (NodeList) path.evaluate(expression, inputSource, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        if (i % columns == 0) {
          employeeJson = new HashMap<>();
          json.add(employeeJson);
        }
        Node node = nodes.item(i);
        employeeJson.put(node.getNodeName(), node.getTextContent());
      }
    } catch (XPathExpressionException e) {
      log.error("Unable to parse xml", e);
    } catch (IOException e) {
      log.error("Unable to read employees.xml at location: '{}'", d);
    }
    return json;
  }
}

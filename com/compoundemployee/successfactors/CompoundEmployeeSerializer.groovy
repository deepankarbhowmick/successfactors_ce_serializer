package com.compoundemployee.successfactors

import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

class CompoundEmployeeSerializer {
    /**
     * Property declaration.
     */
    HashMap<String, ArrayList<HashMap<String, String>>> portletData = new HashMap<String, ArrayList<HashMap<String, String>>>()
    ArrayList<LinkedHashMap<String, String>> finalData = new ArrayList<LinkedHashMap<String, String>>()
    ArrayList<String> date = new ArrayList<String>()
    ArrayList<String> portlet = new ArrayList<String>()
    ArrayList<String> phoneTypeOrder = new ArrayList<String>()
    ArrayList<String> emailTypeOrder = new ArrayList<String>()

    void setPhoneTypeOrder(String... order) {
        order.each({
            String order_ ->
                this.getPhoneTypeOrder().add(order_)
        })
    }

    void setEmailTypeOrder(String... order) {
        order.each({
            String order_ ->
                this.getEmailTypeOrder().add(order_)
        })
    }

    private void explodePortlet(Node node, String portletName) {
        /**
         * Variable declaration.
         */
        HashMap portletDataValue = new HashMap()
        ArrayList<HashMap> portletDataValueList = new ArrayList<HashMap>()

        node.children().each({
            Node child ->
                portletDataValue.put(child.name(), child.text())
        })
        portletDataValueList.add(portletDataValue)
        if (this.getPortletData().containsKey(portletName)) {
            this.getPortletData().get(portletName).add(portletDataValueList.get(0))
        } else {
            this.getPortletData().put(portletName, portletDataValueList)
        }
    }

    private void scanXML(Node node, boolean isDate) {
        /**
         * Variable declaration.
         */
        boolean isDate_

        node.children().each({
            def child ->
                switch (child.getClass().getName().toUpperCase()) {
                    case 'GROOVY.UTIL.NODE':
                        if (this.getPortlet().contains(((child as Node).name() as String).toString())) {
                            /**
                             * Get the entire portlet details.
                             */
                            this.explodePortlet(child as Node, ((child as Node).name() as String).toString())
                            isDate_ = false
                        } else {
                            if (((child as Node).name() as String).toString() == 'start_date') {
                                isDate_ = true
                            } else {
                                isDate_ = false
                            }
                        }
                        this.scanXML(child as Node, isDate_)
                        break
                    default:
                        if (isDate) {
                            if (!this.getDate().contains(child)) {
                                this.getDate().add(child as String)
                            }
                        }
                        break
                }
        })
    }

    private void sortPortletByLMD() {
        this.getPortletData().each({
            String portletName, ArrayList<HashMap<String, String>> portletDataValueList ->
                HashMap<String, HashMap<String, String>> portletDataValueLMD = new HashMap<String, HashMap<String, String>>()
                Date termDate

                /**
                 * Sort the portlet data contents.
                 */
                if (portletDataValueList.size() > 1) {
                    Collections.sort(portletDataValueList, new Comparator<HashMap<String, String>>() {
                        int compare(HashMap<String, String> o1, HashMap<String, String> o2) {
                            String firstValue = o1.get('last_modified_on')
                            String secondValue = o2.get('last_modified_on')
                            return firstValue.compareTo(secondValue)
                        }
                    })
                }
                /**
                 * Now preserve only 1 record for every start date. The records are already sorted in DESC order, so the
                 * most recent modification for the same start_date will be preserved.  However for the JI portlet only
                 * the record with employment status = A has to be preserved if the dates are same.
                 */
                switch (portletName) {
                    case 'job_information':
                        portletDataValueList.each({
                            HashMap<String, String> portletDataValue ->
                                if (portletDataValue.containsKey('start_date')) {
                                    if (portletDataValueLMD.containsKey(portletDataValue.get('start_date') as String)) {
                                        switch (portletDataValueLMD.get(portletDataValue.get('start_date') as String).get(portletDataValueLMD.get('emplStatus') as String)) {
                                            case 'A': //Already the existing record is active, so do not overwrite.
                                                break
                                            default: //The existing record is not A, so overwrite.
                                                portletDataValueLMD.put(portletDataValue.get('start_date') as String, portletDataValue)
                                                break
                                        }
                                    } else {
                                        portletDataValueLMD.put(portletDataValue.get('start_date') as String, portletDataValue)
                                    }
                                }
                        })
                        break
                    case 'employment_information':
                        /**
                         * We need to add 1 to end date if end date is not 9999. Also its safe to make the original
                         * start date as the start date in EI. This will help in rehire situation.
                         */
                        portletDataValueList.each({
                            HashMap<String, String> portletDataValue ->
                                if (portletDataValue.containsKey('end_date')) {
                                    termDate = new SimpleDateFormat('yyyy-MM-dd').parse(portletDataValue.get('end_date') as String)
                                    termDate = termDate.plus(1)
                                    portletDataValue.put('end_date', new SimpleDateFormat('yyyy-MM-dd').format(termDate))
                                }
                        })
                        portletDataValueList.each({
                            HashMap<String, String> portletDataValue ->
                                if (portletDataValue.containsKey('start_date')) {
                                    portletDataValueLMD.put(portletDataValue.get('start_date') as String, portletDataValue)
                                }
                        })
                        break
                    default:
                        portletDataValueList.each({
                            HashMap<String, String> portletDataValue ->
                                if (portletDataValue.containsKey('start_date')) {
                                    portletDataValueLMD.put(portletDataValue.get('start_date') as String, portletDataValue)
                                }
                        })
                        break
                }

                if (portletDataValueLMD.size() > 0) {
                    this.getPortletData().get(portletName).removeAll(this.getPortletData().get(portletName))
                    portletDataValueLMD.each({
                        String date, HashMap<String, String> portletDataValue ->
                            this.getPortletData().get(portletName).add(portletDataValue)
                    })
                }
        })
    }

    private void orderPortlet(String portletName) {
        //Variable declaration.
        HashMap<String, String> result

        /**
         * Special processing for phone-type and email-type.
         */
        switch (portletName) {
            case 'phone_information':
                this.getPhoneTypeOrder().each({
                    String phoneType ->
                        if (result == null) { //Continue to search till the result is initialized.
                            this.getPortletData().get(portletName).each({
                                HashMap<String, String> portletDataValue ->
                                    if (portletDataValue.get('phone_type') == phoneType) {
                                        result = portletDataValue
                                    }
                            })
                        }
                })
                break
            case 'email_information':
                this.getEmailTypeOrder().each({
                    String emailType ->
                        if (result == null) { //Continue to search till the result is initialized.
                            this.getPortletData().get(portletName).each({
                                HashMap<String, String> portletDataValue ->
                                    if (portletDataValue.get('email_type') == emailType) {
                                        result = portletDataValue
                                    }
                            })
                        }
                })
                break
            default:
                break
        }
        /**
         * Now overwrite the portlet with result.
         */
        if (result != null) {
            this.getPortletData().get(portletName).removeAll(this.getPortletData().get(portletName))
            this.getPortletData().get(portletName).add(result)
        }
    }

    String serialize(String compoundEmployee, String configuration, String effDateName) throws CompoundEmployeeException {
        /**
         * Variable declaration.
         */
        Node root, config
        MarkupBuilder xml
        StringWriter stringWriter
        ArrayList<String> eiStartsDate = new ArrayList<String>()

        try {
            root = new XmlParser().parseText(compoundEmployee)
            config = new XmlParser().parseText(configuration)
            stringWriter = new StringWriter()
            xml = new MarkupBuilder(stringWriter)

            /**
             * Get the portlet name from the configuration for which data needs to be extracted.
             */
            config.children().each({
                Node child ->
                    if (!this.getPortlet().contains(child.name() as String)) {
                        this.getPortlet().add(child.name() as String)
                    }
            })

            /**
             * Parse the CE.
             */
            this.scanXML(root, false)
            this.sortPortletByLMD()
            this.orderPortlet('phone_information')
            this.orderPortlet('email_information')
            Collections.sort(this.getDate())

            /**
             * Collect the start dates of the employment information.
             */
            this.getPortletData().get('employment_information').each({
                HashMap employmentInformation ->
                    eiStartsDate.add(employmentInformation.get('start_date') as String)
            })

            /**
             * Construct n rows with the dates.
             */
            this.getDate().each({
                String singleDate ->
                    LinkedHashMap<String, String> finalDataValue = new LinkedHashMap<String, String>()

                    /**
                     * Check if this employee is rehired. If yes then the previous employment should not be sent.
                     */
                    if (!this.keepRecord(eiStartsDate, singleDate)) {
                        return
                    }
                    finalDataValue.put(effDateName, singleDate)
                    config.children().each({
                        Node child ->
                            finalDataValue.put(child.text(), '') //Translated value.
                    })
                    this.getFinalData().add(finalDataValue)
            })

            /**
             * Now for each date there should be a record from all the portlets. And only the records whose date
             * match should be populated in the record.
             */
            this.getFinalData().each({
                LinkedHashMap<String, String> finalDataValue ->
                    this.getPortletData().each({
                        String portletName, ArrayList<HashMap<String, String>> portletDataValueList ->
                            portletDataValueList.each({
                                HashMap<String, String> portletDataValue ->
                                    String startDate, endDate, compareDate
                                    if (portletDataValue.containsKey('start_date')) {
                                        /**
                                         * Get the dates.
                                         */
                                        startDate = portletDataValue.get('start_date').toString()
                                        compareDate = finalDataValue.get(effDateName).toString()
                                        if (portletDataValue.containsKey('end_date')) {
                                            endDate = portletDataValue.get('end_date').toString()
                                        } else {
                                            endDate = '9999-12-31'
                                        }
                                        /**
                                         * Compare the dates.
                                         */
                                        if ((new SimpleDateFormat('yyyy-MM-dd').parse(startDate) <=
                                                new SimpleDateFormat('yyyy-MM-dd').parse(compareDate)) &&
                                                (new SimpleDateFormat('yyyy-MM-dd').parse(endDate) >=
                                                        new SimpleDateFormat('yyyy-MM-dd').parse(compareDate))) {
                                            portletDataValue.each({
                                                String key, value ->
                                                    String translatedValue
                                                    /**
                                                     * Get the translated field-name of this portlet.
                                                     */
                                                    translatedValue = this.getTranslatedValue(portletName, key, configuration)
                                                    if (translatedValue != null) {
                                                        if (finalDataValue.containsKey(translatedValue)) {
                                                            finalDataValue.put(translatedValue, value)
                                                        }
                                                    }
                                            })
                                        }
                                    } else {
                                        portletDataValue.each({
                                            String key, value ->
                                                String translatedValue
                                                /**
                                                 * Get the translated field-name of this portlet.
                                                 */
                                                translatedValue = this.getTranslatedValue(portletName, key, configuration)
                                                if (translatedValue != null) {
                                                    if (finalDataValue.containsKey(translatedValue)) {
                                                        finalDataValue.put(translatedValue, value)
                                                    }
                                                }
                                        })
                                    }
                            })
                    })
            })

            xml.'root'({
                this.getFinalData().each({
                    HashMap record ->
                        xml.'record'({
                            record.each({
                                String key, value ->
                                    xml."${key}"(value)
                            })
                        })
                })
            })
            return stringWriter.toString()
        }
        catch (Exception exception) {
            throw new CompoundEmployeeException(exception.getMessage())
        }
    }

    boolean keepRecord(ArrayList<String> startDate, String singleDate) {
        /**
         * Variable declaration.
         */
        boolean afterEmploymentStartDate = false

        if (startDate.size() > 0) {
            /**
             * Compare the dates.
             */
            startDate.each({
                String startDate_ ->
                    if (new SimpleDateFormat('yyyy-MM-dd').parse(singleDate) >=
                            new SimpleDateFormat('yyyy-MM-dd').parse(startDate_)) {
                        afterEmploymentStartDate = true
                    }
            })
            return afterEmploymentStartDate
        }
    }

    private String getTranslatedValue(String portletName, String portletField, String configuration) {
        /**
         * Variable declaration.
         */
        Node config = new XmlParser().parseText(configuration)
        String translatedValue

        config.children().each({
            Node child ->
                if (child.name() == portletName && (child.attribute('field') as String) == portletField) {
                    translatedValue = child.text()
                }
        })
        return translatedValue
    }

    void debug(String employeeFile, String configFile) {
        File file
        String employeeFileContent, configFileContent, parse

        employeeFileContent = ""
        configFileContent = ""
        /**
         * Read the employee file.
         */
        file = new File(employeeFile)
        file.readLines().each({
            String line ->
                employeeFileContent = employeeFileContent + line
        })

        /**
         * Read the config file.
         */
        file = new File(configFile)
        file.readLines().each({
            String line ->
                configFileContent = configFileContent + line
        })

        /**
         * Perform testing.
         */
        this.setPhoneTypeOrder('B', 'C', 'BI') //Phone type precedence.
        this.setEmailTypeOrder('B', 'P') //Email type precedence.
        parse = this.serialize(employeeFileContent, configFileContent, 'EFF_DT')
        println(parse)
    }

    //Activate only to test the process.
//    static void main(String[] args) {
//        String employeeFile = '<path to the employee xml file>'
//        String configFile = '<path to the configuration xml file>'
//        new CompoundEmployeeSerializer().debug(employeeFile, configFile)
//    }
}

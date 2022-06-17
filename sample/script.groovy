def Message parseEmployee(Message message) {
    //Variable declaration.
    def employee = message.getBody(java.lang.String) as String
    def messageLog = messageLogFactory.getMessageLog(message)
    def configuration = message.getProperty('Configuration')
    CompoundEmployeeSerializer ceSerializer = new CompoundEmployeeSerializer()
    String parse
    
    try{
        ceSerializer.setPhoneTypeOrder('B', 'C', 'BI') //Phone type precedence.
        ceSerializer.setEmailTypeOrder('B', 'P') //Email type precedence.
        parse = ceSerializer.serialize(employee, configuration, 'EFF_DT')
    }
    catch(CompoundEmployeeException exception){
        parse = '<result>' + exception.getMessage() + '</result>'
        if(messageLog != null){
            messageLog.addAttachmentAsString("EXCEPTION_MESSAGE", parse, "text/plain")
        }
    }
    message.setBody(parse)
    return message
}

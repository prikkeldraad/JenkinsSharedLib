#!/usr/bin/env groovy
package nl.prikkeldraad.jenkins
import nl.prikkeldraad.jenkins.*

class Git implements Serializable  {
    /**
    * @var String
    */
    public url

    /**
    * @var String
    */
    private project
    
    /**
    * @var String
    * Token username:password
    */
    public token
    
    /**
    * @var String
    * Container to store Jenkinsfile context
    */
    public context
    
    /**
    * @param String url
    * @param String project name
    */
    public Git (String url, String project) {
        this.url = url
        this.project = project
    }
    
    /**
    * Generate an url with the token
    * @return String
    * @todo make sure token is set
    */
    private getTokenURL() {
        // insert token after protocol before url
        def parts = this.url.split("://")
        return sprintf("%s://%s@%s", [parts[0], this.token, parts[1]])
    }
    
    /**
    * With Jenkinsfile context use that echo statement, else use print
    * @param string to print
    */
    private echo (String str) {
        if (this.context) {
            this.context.echo str
        } else {
            print str
        }
    }
    
    /**
    * Run cmd in bash
    * @param string bash command
    */
    private bashCmd(cmd) {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        
        def proc = new ProcessBuilder(
            "bash",
            "-c",
            cmd
        ).start()
            
        // @todo do something with errors
        proc.consumeProcessOutput(stdout, stderr)
        proc.waitFor()
        
        if (stderr) {
            throw new Exception(stderr.toString())
        }

        return stdout
    }

    /**
    * Get all the tags from the git repo
    * @return List<String> version numbers 
    */
    private getTags() {
        this.echo "[ DEBUG ] getTags(): git ls-remote --tags ${this.getTokenURL()}| cut -d'/' -f3"
        
        def stdout = this.bashCmd(" \
            git ls-remote --tags ${this.getTokenURL()}| \
            cut -d'/' -f3")
            //grep -o '[[:digit:]]\.[[:digit:]]\.[[:digit:]]'")
            
        this.echo "[ DEBUG ] getTags(): ${stdout.readLines()}"
        
        def tags = [:]
        stdout.readLines().each { row ->
            try {
                def matches = (row =~ /^(\d+\.\d+\.\d+)(-\d+)?$/)        
                tags[row] = matches[0][1]
            } catch (IndexOutOfBoundsException e) {
                // fancy groovy syntax to continue in this weird each loop
                return
            }
        }
                
        this.echo "[ DEBUG ] getTags(): ${tags}"
        return tags
    }
    
    /**
    * Get issuenumbers from log messages between revisions
    * @param Integer revision number
    * @param Integer revision number 
    * @param prefix filter for log messages (ea project name)
    * @return List <Integer> issue numbers
    */
    private getLog(rev1, rev2, prefix) {
        def check = "(?i)${prefix}-[0-9]+"
        
        this.echo "[ DEBUG ] getLog(): git log --pretty=oneline ${rev1}...${rev2} --format=%s"
        def stdout = this.bashCmd("git log --pretty=oneline ${rev1}...${rev2} --format=%s")    
        this.echo "[ DEBUG ] getLog(): ${stdout.toString()}"
        
        def numbers = []
        
        stdout.each { entry ->
            try {
                def matches = (entry =~ /${check}/)
                numbers.add(matches[0])
            } catch (java.lang.IndexOutOfBoundsException e) {
                this.echo "[ WARNING ] No match in: ${entry.toString()}\n"
            }
        }
        
        return numbers
    }
    
    /**
    * Get the one before this version
    * @todo bad title, actually returns n-1 and not n
    * @todo should be testsed with tags with build numbers
    */
    private getLatestItem(tags) {
        def latest = tags[tags.values().unique().sort()[-2]]
        def tag = ''
        def bnr = 0
        tags.each { key, value ->
            if (value == latest) {
                try {                
                    if (key.split("-")[1] > bnr) {
                        bnr = key.split("-")[1]
                        tag = key
                    }
                } catch (java.lang.IndexOutOfBoundsException e) {
                    // no build id found
                    return
                }
            }
        }
        
        if (tag == '') {
            return latest
        } else {
            return tag
        }
    }

    /**
    * Wrapper to retrieve issue numbers between revisions
    * @return List <Integer> issue numbers
    */
    public getIssueNumbers(current_tag) {
        def tags = this.getTags()
        this.echo "[ DEBUG ] Tags: ${tags}"

        def max = this.getLatestItem(tags)
        this.echo "[ DEBUG ] Max: ${max}"
        
        def log = this.getLog(current_tag, max, this.project)
        this.echo "[ DEBUG ] Log: ${log}"
        
        return log.unique().sort().reverse()
    }
}

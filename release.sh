!/bin/bash
# Deploy maven artefact in current directory into Maven central repository
# using maven-release-plugin goals
read -p "Really deploy to maven central repository  (yes/no)? "
if ( [ "$REPLY" == "yes" ] ) then
  GPG_TTY=$(tty)
  export GPG_TTY
  mvn -DskipTests release:prepare release:perform
else
  echo 'Exit without deploy'
fi
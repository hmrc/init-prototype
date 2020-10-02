#!/usr/bin/env bash
if [[ $# -ne 1 ]] ; then
    echo 'Usage: compare-repositories.sh <heroku-app-list>'
    exit 1
fi

# Remove header row and extract first two columns
prototypes=$(tail -n +2 "$1" | cut -f1,2)

echo -e "herokuApp\thmrcRepository\tisSynced"

# Split by newline and iterate over each line
IFS=$'\n'
for heroku_app_hmrc_repo in $prototypes; do
  heroku_app=$(echo "$heroku_app_hmrc_repo" | cut -f1)
  hmrc_repo=$(echo "$heroku_app_hmrc_repo" | cut -f2)
  is_synced="FALSE"

  if [[ "$hmrc_repo" ]]; then
    if [[ ! -d "$hmrc_repo" ]]; then
      >&2 echo "INFO: Cloning HMRC repo $hmrc_repo"
      git clone --quiet https://github.com/hmrc/"$hmrc_repo".git || exit
    else
      >&2 echo "INFO: Found HMRC repo $hmrc_repo"
    fi

    cd "$hmrc_repo" || exit

    >&2 echo "INFO: Fetching remote for Heroku app $heroku_app"
    git remote remove heroku 2>/dev/null
    git remote add heroku https://git.heroku.com/"$heroku_app".git && \
    git fetch --all --quiet || exit

    >&2 echo "INFO: Comparing HMRC repo $hmrc_repo with Heroku app $heroku_app"
    if [[ ! $(git diff origin/master..heroku/master) ]]; then
      is_synced="TRUE"
    fi

    cd ..
  fi

  echo -e "$heroku_app_hmrc_repo\t$is_synced"
done

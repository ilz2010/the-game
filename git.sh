git filter-branch -f --commit-filter 'export GIT_AUTHOR_NAME="ilz2010"; export GIT_AUTHOR_EMAIL=ilz2010@yandex.ru; git commit-tree "$@"'